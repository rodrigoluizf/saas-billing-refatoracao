package com.saas.billing.domain;

/** Resultado de uma tentativa de cobrança no cartão. */
public record PaymentResult(boolean approved) {

    public static PaymentResult approvedPayment() {
        return new PaymentResult(true);
    }

    public static PaymentResult declinedPayment() {
        return new PaymentResult(false);
    }
}
