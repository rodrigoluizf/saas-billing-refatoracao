package com.saas.billing.port;

import com.saas.billing.domain.Invoice;

/** Porta de saída: persistência de faturas. */
public interface InvoiceRepository {

    /** @throws BillingInfrastructureException se a fatura não puder ser gravada */
    void save(Invoice invoice);
}
