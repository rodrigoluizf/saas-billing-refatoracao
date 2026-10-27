package com.saas.billing;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.Invoice;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.domain.PaymentResult;
import com.saas.billing.port.BillingInfrastructureException;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.InvoiceRepository;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.PaymentGateway;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SubscriptionBillingService {

    private static final System.Logger LOGGER = System.getLogger(SubscriptionBillingService.class.getName());

    private static final BigDecimal LOYALTY_DISCOUNT_AFTER_24_MONTHS = new BigDecimal("0.15");
    private static final BigDecimal LOYALTY_DISCOUNT_AFTER_12_MONTHS = new BigDecimal("0.10");
    private static final BigDecimal NON_PROFIT_PRICE_FACTOR = new BigDecimal("0.70");
    private static final Money BLACK_FRIDAY_DISCOUNT = Money.of("30.00");
    private static final BigDecimal WELCOME10_PRICE_FACTOR = new BigDecimal("0.90");
    private static final BigDecimal LATE_PAYMENT_PENALTY_RATE = new BigDecimal("0.02");
    private static final BigDecimal DAILY_INTEREST_RATE = new BigDecimal("0.001");

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
    private final Clock clock;

    public SubscriptionBillingService(CustomerRepository customerRepository,
                                      InvoiceRepository invoiceRepository,
                                      PaymentGateway paymentGateway,
                                      NotificationService notificationService,
                                      Clock clock) {
        this.customerRepository = Objects.requireNonNull(customerRepository);
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository);
        this.paymentGateway = Objects.requireNonNull(paymentGateway);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Assinatura legada mantida para não quebrar os chamadores existentes.
     *
     * @deprecated use {@link #processMonthlyBilling(BillingCommand)}. O parâmetro {@code planType}
     *     nunca foi utilizado e é ignorado.
     */
    @Deprecated
    public List<String> processMonthlyBilling(String customerId, String planType, double basePrice,
                                             String signupDateStr, int overdueDays, String promoCode,
                                             boolean isNonProfit, String paymentCardToken) {
        return processMonthlyBilling(BillingCommand.builder()
                .customerId(customerId)
                .basePrice(Money.of(basePrice))
                .signupDate(LocalDate.parse(signupDateStr))
                .overdueDays(overdueDays)
                .promoCode(promoCode)
                .nonProfit(isNonProfit)
                .paymentCardToken(paymentCardToken)
                .build());
    }

    public List<String> processMonthlyBilling(BillingCommand command) {

        List<String> logs = new ArrayList<>();
        String customerId = command.customerId();

        LocalDate signupDate = command.signupDate();
        LocalDate today = LocalDate.now(clock);

        int monthsActive = (today.getYear() - signupDate.getYear()) * 12 + (today.getMonthValue() - signupDate.getMonthValue());

        Money basePrice = command.basePrice();
        Money calculatedPrice = basePrice;

        if (monthsActive > 24) {
            calculatedPrice = calculatedPrice.minus(basePrice.times(LOYALTY_DISCOUNT_AFTER_24_MONTHS));
        } else if (monthsActive > 12) {
            calculatedPrice = calculatedPrice.minus(basePrice.times(LOYALTY_DISCOUNT_AFTER_12_MONTHS));
        }

        if (command.nonProfit()) {
            calculatedPrice = calculatedPrice.times(NON_PROFIT_PRICE_FACTOR);
        }

        if (command.hasPromoCode()) {
            String promoCode = command.promoCode();
            if (promoCode.equalsIgnoreCase("BLACKFRIDAY") && today.getMonthValue() == 11) {
                calculatedPrice = calculatedPrice.minus(BLACK_FRIDAY_DISCOUNT);
            } else if (promoCode.equalsIgnoreCase("WELCOME10") && monthsActive <= 1) {
                calculatedPrice = calculatedPrice.times(WELCOME10_PRICE_FACTOR);
            }
        }

        if (command.isOverdue()) {
            Money penalty = calculatedPrice.times(LATE_PAYMENT_PENALTY_RATE);
            Money dailyInterest = calculatedPrice.times(DAILY_INTEREST_RATE).times(command.overdueDays());
            calculatedPrice = calculatedPrice.plus(penalty).plus(dailyInterest);
        }

        Money finalPrice = calculatedPrice.atLeastZero().rounded();

        try {
            Optional<Customer> found = customerRepository.findById(customerId);
            if (found.isPresent()) {
                Customer customer = found.get();

                if (!customer.isActive()) {
                    logs.add("Cliente inativo. Cobrança cancelada: " + customerId);
                    return logs;
                }

                PaymentResult payment = paymentGateway.charge(command.paymentCardToken(), finalPrice);
                if (payment.approved()) {
                    logs.add("Pagamento processado com sucesso: R$ " + finalPrice);
                } else {
                    logs.add("Falha no pagamento do cartão para: " + customerId);
                }

                invoiceRepository.save(new Invoice(customerId, finalPrice, InvoiceStatus.from(payment)));

                notificationService.sendInvoiceReceipt(customer.email(), finalPrice);
                logs.add("E-mail disparado para: " + customer.email());
            }
        } catch (BillingInfrastructureException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Falha de infraestrutura na cobrança do cliente " + customerId, e);
            logs.add("Erro fatal no processamento: " + e.getMessage());
        }

        return logs;
    }
}
