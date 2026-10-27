package com.saas.billing.port;

import com.saas.billing.domain.Money;
import com.saas.billing.domain.PaymentResult;

/** Porta de saída: cobrança no cartão de crédito. */
public interface PaymentGateway {

    /**
     * @param cardToken token do cartão (pode ser nulo: cobrança recusada)
     * @param amount    valor já arredondado a centavos
     * @throws BillingInfrastructureException se o gateway estiver indisponível
     */
    PaymentResult charge(String cardToken, Money amount);
}
