package com.saas.billing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Objects;

/**
 * Motor de precificação puro: só regras de negócio, sem rede, banco, relógio ou estado.
 * Mesma entrada produz sempre a mesma saída.
 *
 * <p>A ordem das regras é a do código legado e faz parte do comportamento:
 * <ol>
 *   <li>desconto de fidelidade sobre o preço base (10% acima de 12 meses, 15% acima de 24);</li>
 *   <li>desconto de 30% para ONGs;</li>
 *   <li>cupom promocional (BLACKFRIDAY: R$ 30 só em novembro; WELCOME10: 10% até 1 mês de casa);</li>
 *   <li>encargos de atraso: multa de 2% + juros de 0,1% ao dia;</li>
 *   <li>piso em zero e arredondamento a centavos.</li>
 * </ol>
 */
public final class PricingCalculator {

    static final int LONG_LOYALTY_MONTHS = 24;
    static final int SHORT_LOYALTY_MONTHS = 12;
    static final BigDecimal LONG_LOYALTY_DISCOUNT = new BigDecimal("0.15");
    static final BigDecimal SHORT_LOYALTY_DISCOUNT = new BigDecimal("0.10");

    static final BigDecimal NON_PROFIT_PRICE_FACTOR = new BigDecimal("0.70");

    static final String BLACK_FRIDAY_CODE = "BLACKFRIDAY";
    static final Month BLACK_FRIDAY_MONTH = Month.NOVEMBER;
    static final Money BLACK_FRIDAY_DISCOUNT = Money.of("30.00");

    static final String WELCOME_CODE = "WELCOME10";
    static final int WELCOME_MAX_MONTHS = 1;
    static final BigDecimal WELCOME_PRICE_FACTOR = new BigDecimal("0.90");

    static final BigDecimal LATE_PAYMENT_PENALTY_RATE = new BigDecimal("0.02");
    static final BigDecimal DAILY_INTEREST_RATE = new BigDecimal("0.001");

    /**
     * @param command       dados da cobrança
     * @param referenceDate "hoje", fornecido por quem chama (o serviço usa o {@code Clock} injetado)
     * @return valor final a cobrar, nunca negativo, arredondado a centavos
     */
    public Money calculate(BillingCommand command, LocalDate referenceDate) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(referenceDate, "referenceDate");

        int monthsActive = monthsActive(command.signupDate(), referenceDate);

        Money price = applyLoyaltyDiscount(command.basePrice(), monthsActive);
        price = applyNonProfitDiscount(price, command.nonProfit());
        price = applyPromoCode(price, command, referenceDate, monthsActive);
        price = applyLatePaymentCharges(price, command.overdueDays());
        return price.atLeastZero().rounded();
    }

    /**
     * Diferença em meses de calendário, ignorando o dia (regra do legado). Atenção: não é
     * {@code ChronoUnit.MONTHS.between}, que considera o dia e mudaria o resultado.
     * Pode ser negativa se a adesão for futura.
     */
    static int monthsActive(LocalDate signupDate, LocalDate referenceDate) {
        return (referenceDate.getYear() - signupDate.getYear()) * 12
                + (referenceDate.getMonthValue() - signupDate.getMonthValue());
    }

    private static Money applyLoyaltyDiscount(Money basePrice, int monthsActive) {
        if (monthsActive > LONG_LOYALTY_MONTHS) {
            return basePrice.minus(basePrice.times(LONG_LOYALTY_DISCOUNT));
        }
        if (monthsActive > SHORT_LOYALTY_MONTHS) {
            return basePrice.minus(basePrice.times(SHORT_LOYALTY_DISCOUNT));
        }
        return basePrice;
    }

    private static Money applyNonProfitDiscount(Money price, boolean nonProfit) {
        return nonProfit ? price.times(NON_PROFIT_PRICE_FACTOR) : price;
    }

    /**
     * O legado testa "cupom em branco" com {@code trim()}, mas compara o código sem
     * {@code trim()}: " BLACKFRIDAY" não é reconhecido. Comportamento preservado.
     */
    private static Money applyPromoCode(Money price, BillingCommand command, LocalDate referenceDate,
                                        int monthsActive) {
        if (!command.hasPromoCode()) {
            return price;
        }
        String code = command.promoCode();
        if (code.equalsIgnoreCase(BLACK_FRIDAY_CODE) && referenceDate.getMonth() == BLACK_FRIDAY_MONTH) {
            return price.minus(BLACK_FRIDAY_DISCOUNT);
        }
        if (code.equalsIgnoreCase(WELCOME_CODE) && monthsActive <= WELCOME_MAX_MONTHS) {
            return price.times(WELCOME_PRICE_FACTOR);
        }
        return price;
    }

    private static Money applyLatePaymentCharges(Money price, int overdueDays) {
        if (overdueDays <= 0) {
            return price;
        }
        Money penalty = price.times(LATE_PAYMENT_PENALTY_RATE);
        Money interest = price.times(DAILY_INTEREST_RATE).times(overdueDays);
        return price.plus(penalty).plus(interest);
    }
}
