package com.saas.billing.port;

import com.saas.billing.domain.Money;

/** Porta de saída: envio do recibo da fatura ao cliente. */
public interface NotificationService {

    /** @throws BillingInfrastructureException se a mensagem não puder ser montada ou enviada */
    void sendInvoiceReceipt(String email, Money amount);
}
