package com.saas.billing.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import org.junit.Test;

public class BillingCommandTest {

    private BillingCommand.Builder valid() {
        return BillingCommand.builder()
                .customerId("cus_1")
                .basePrice(Money.of("100.00"))
                .signupDate(LocalDate.of(2024, 1, 10));
    }

    @Test
    public void builderMontaTodosOsCampos() {
        BillingCommand cmd = valid().overdueDays(3).promoCode("WELCOME10")
                .nonProfit(true).paymentCardToken("tok_abc").build();

        assertEquals("cus_1", cmd.customerId());
        assertEquals(Money.of("100.00"), cmd.basePrice());
        assertEquals(LocalDate.of(2024, 1, 10), cmd.signupDate());
        assertEquals(3, cmd.overdueDays());
        assertEquals("WELCOME10", cmd.promoCode());
        assertTrue(cmd.nonProfit());
        assertEquals("tok_abc", cmd.paymentCardToken());
    }

    @Test
    public void cupomEmBrancoOuNuloNaoContaComoCupom() {
        assertFalse(valid().promoCode(null).build().hasPromoCode());
        assertFalse(valid().promoCode("   ").build().hasPromoCode());
        assertTrue(valid().promoCode("BLACKFRIDAY").build().hasPromoCode());
    }

    @Test
    public void atrasoSoExisteComDiasPositivos() {
        assertFalse(valid().overdueDays(0).build().isOverdue());
        assertFalse(valid().overdueDays(-5).build().isOverdue());
        assertTrue(valid().overdueDays(1).build().isOverdue());
    }

    @Test
    public void precoEDataDeAdesaoSaoObrigatorios() {
        assertThrows(NullPointerException.class, () -> valid().basePrice(null).build());
        assertThrows(NullPointerException.class, () -> valid().signupDate(null).build());
    }

    @Test
    public void ehUmValueObject() {
        assertEquals(valid().build(), valid().build());
    }
}
