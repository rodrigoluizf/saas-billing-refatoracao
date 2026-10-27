package com.saas.billing.domain;

import java.util.Objects;

/** Fatura mensal a ser persistida pela porta {@link com.saas.billing.port.InvoiceRepository}. */
public record Invoice(String customerId, Money amount, InvoiceStatus status) {

    public Invoice {
        Objects.requireNonNull(amount, "amount é obrigatório");
        Objects.requireNonNull(status, "status é obrigatório");
    }
}
