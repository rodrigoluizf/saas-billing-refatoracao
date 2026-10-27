package com.saas.billing.domain;

/** Status gravado na fatura. Os nomes são os mesmos valores que o legado gravava no banco. */
public enum InvoiceStatus {
    PAID,
    FAILED;

    public static InvoiceStatus from(PaymentResult payment) {
        return payment.approved() ? PAID : FAILED;
    }
}
