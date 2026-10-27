package com.saas.billing;

import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Customer;
import com.saas.billing.domain.Invoice;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.domain.PaymentResult;
import com.saas.billing.domain.PricingCalculator;
import com.saas.billing.port.BillingInfrastructureException;
import com.saas.billing.port.CustomerRepository;
import com.saas.billing.port.InvoiceRepository;
import com.saas.billing.port.NotificationService;
import com.saas.billing.port.PaymentGateway;
import java.lang.System.Logger.Level;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fachada de aplicação da cobrança mensal: orquestra precificação, pagamento, fatura e
 * recibo. Não contém regra de preço (está em {@link PricingCalculator}) nem I/O concreto
 * (está atrás das portas). A lista de mensagens retornada é a mesma do código legado.
 */
public class SubscriptionBillingService {

    private static final System.Logger LOGGER = System.getLogger(SubscriptionBillingService.class.getName());

    static final String INACTIVE_CUSTOMER = "Cliente inativo. Cobrança cancelada: ";
    static final String PAYMENT_APPROVED = "Pagamento processado com sucesso: R$ ";
    static final String PAYMENT_DECLINED = "Falha no pagamento do cartão para: ";
    static final String RECEIPT_SENT = "E-mail disparado para: ";
    static final String FATAL_ERROR = "Erro fatal no processamento: ";

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
    private final PricingCalculator pricingCalculator;
    private final Clock clock;

    public SubscriptionBillingService(CustomerRepository customerRepository,
                                      InvoiceRepository invoiceRepository,
                                      PaymentGateway paymentGateway,
                                      NotificationService notificationService,
                                      Clock clock) {
        this(customerRepository, invoiceRepository, paymentGateway, notificationService,
                new PricingCalculator(), clock);
    }

    public SubscriptionBillingService(CustomerRepository customerRepository,
                                      InvoiceRepository invoiceRepository,
                                      PaymentGateway paymentGateway,
                                      NotificationService notificationService,
                                      PricingCalculator pricingCalculator,
                                      Clock clock) {
        this.customerRepository = Objects.requireNonNull(customerRepository);
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository);
        this.paymentGateway = Objects.requireNonNull(paymentGateway);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.pricingCalculator = Objects.requireNonNull(pricingCalculator);
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
        Money amount = pricingCalculator.calculate(command, LocalDate.now(clock));
        List<String> logs = new ArrayList<>();
        try {
            customerRepository.findById(command.customerId())
                    .ifPresent(customer -> bill(customer, command, amount, logs));
        } catch (BillingInfrastructureException e) {
            LOGGER.log(Level.ERROR, "Falha de infraestrutura na cobrança do cliente " + command.customerId(), e);
            logs.add(FATAL_ERROR + e.getMessage());
        }
        return logs;
    }

    private void bill(Customer customer, BillingCommand command, Money amount, List<String> logs) {
        if (!customer.isActive()) {
            logs.add(INACTIVE_CUSTOMER + command.customerId());
            return;
        }
        PaymentResult payment = paymentGateway.charge(command.paymentCardToken(), amount);
        logs.add(payment.approved() ? PAYMENT_APPROVED + amount : PAYMENT_DECLINED + command.customerId());
        invoiceRepository.save(new Invoice(command.customerId(), amount, InvoiceStatus.from(payment)));
        notificationService.sendInvoiceReceipt(customer.email(), amount);
        logs.add(RECEIPT_SENT + customer.email());
    }
}
