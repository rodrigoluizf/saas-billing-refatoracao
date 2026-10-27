package com.saas.billing.adapter.payment;

import com.saas.billing.domain.Money;
import com.saas.billing.domain.PaymentResult;
import com.saas.billing.port.PaymentGateway;

/**
 * Adaptador que reproduz a regra de pagamento que estava embutida no serviço legado:
 * a cobrança é aprovada se, e somente se, o token do cartão começar com {@code "tok_"}.
 *
 * <p>Não há chamada real a adquirente. Quando o gateway real for integrado, basta criar
 * outro adaptador de {@link PaymentGateway} e trocá-lo na composição
 * ({@link com.saas.billing.config.BillingServiceFactory}), sem tocar no domínio.
 */
public class LegacyTokenPaymentGateway implements PaymentGateway {

    static final String VALID_TOKEN_PREFIX = "tok_";

    @Override
    public PaymentResult charge(String cardToken, Money amount) {
        boolean approved = cardToken != null && cardToken.startsWith(VALID_TOKEN_PREFIX);
        return approved ? PaymentResult.approvedPayment() : PaymentResult.declinedPayment();
    }
}
