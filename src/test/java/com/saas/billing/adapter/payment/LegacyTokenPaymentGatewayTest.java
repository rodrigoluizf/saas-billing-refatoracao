package com.saas.billing.adapter.payment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.saas.billing.domain.Money;
import org.junit.Test;

public class LegacyTokenPaymentGatewayTest {

    private final LegacyTokenPaymentGateway gateway = new LegacyTokenPaymentGateway();
    private final Money amount = Money.of("10.00");

    @Test
    public void aprovaTokenComPrefixoTok() {
        assertTrue(gateway.charge("tok_123", amount).approved());
    }

    @Test
    public void recusaTokenSemPrefixoNuloOuComMaiusculas() {
        assertFalse(gateway.charge("card_123", amount).approved());
        assertFalse(gateway.charge(null, amount).approved());
        assertFalse(gateway.charge("TOK_123", amount).approved());
        assertFalse(gateway.charge("", amount).approved());
    }
}
