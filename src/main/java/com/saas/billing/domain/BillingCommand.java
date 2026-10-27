package com.saas.billing.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Parameter Object imutável com tudo o que é necessário para processar a cobrança mensal
 * de um cliente. Substitui a lista de 8 parâmetros soltos do método legado.
 *
 * <p>O antigo parâmetro {@code planType} foi removido: ele nunca era lido pelo legado
 * (parâmetro morto). As regras de entrada do legado foram preservadas — {@code customerId},
 * {@code promoCode} e {@code paymentCardToken} continuam aceitando {@code null}, e
 * {@code overdueDays <= 0} continua significando "sem atraso".
 */
public record BillingCommand(
        String customerId,
        Money basePrice,
        LocalDate signupDate,
        int overdueDays,
        String promoCode,
        boolean nonProfit,
        String paymentCardToken) {

    public BillingCommand {
        Objects.requireNonNull(basePrice, "basePrice é obrigatório");
        Objects.requireNonNull(signupDate, "signupDate é obrigatório");
    }

    public boolean hasPromoCode() {
        return promoCode != null && !promoCode.trim().isEmpty();
    }

    public boolean isOverdue() {
        return overdueDays > 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder para deixar a montagem do comando legível (evita 7 argumentos posicionais). */
    public static final class Builder {
        private String customerId;
        private Money basePrice;
        private LocalDate signupDate;
        private int overdueDays;
        private String promoCode;
        private boolean nonProfit;
        private String paymentCardToken;

        private Builder() {
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder basePrice(Money basePrice) {
            this.basePrice = basePrice;
            return this;
        }

        public Builder signupDate(LocalDate signupDate) {
            this.signupDate = signupDate;
            return this;
        }

        public Builder overdueDays(int overdueDays) {
            this.overdueDays = overdueDays;
            return this;
        }

        public Builder promoCode(String promoCode) {
            this.promoCode = promoCode;
            return this;
        }

        public Builder nonProfit(boolean nonProfit) {
            this.nonProfit = nonProfit;
            return this;
        }

        public Builder paymentCardToken(String paymentCardToken) {
            this.paymentCardToken = paymentCardToken;
            return this;
        }

        public BillingCommand build() {
            return new BillingCommand(customerId, basePrice, signupDate, overdueDays,
                    promoCode, nonProfit, paymentCardToken);
        }
    }
}
