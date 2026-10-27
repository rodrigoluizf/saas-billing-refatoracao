package com.saas.billing.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import org.junit.Test;

public class MoneyTest {

    @Test
    public void arredondaComDuasCasasEHalfEven() {
        assertEquals("10.12", Money.of("10.125").rounded().toString()); // 2 é par: mantém
        assertEquals("10.14", Money.of("10.135").rounded().toString()); // 3 é ímpar: sobe
        assertEquals("10.13", Money.of("10.1251").rounded().toString());
    }

    @Test
    public void naoAcumulaErroDePontoFlutuante() {
        // Em double: 0.1 + 0.2 = 0.30000000000000004
        assertEquals(Money.of("0.30"), Money.of(0.1).plus(Money.of(0.2)));
        assertEquals("0.30", Money.of(0.1).plus(Money.of(0.2)).toString());
    }

    @Test
    public void ofDoubleUsaRepresentacaoDecimalMaisCurta() {
        assertEquals(new BigDecimal("99.99"), Money.of(99.99).amount());
    }

    @Test
    public void ofDoubleRejeitaNaNEInfinito() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Money.of(Double.POSITIVE_INFINITY));
    }

    @Test
    public void operacoesSaoExatasAteOArredondamentoFinal() {
        Money price = Money.of("99.99").times(new BigDecimal("0.85"));
        assertEquals(new BigDecimal("84.9915"), price.amount());
        assertEquals("84.99", price.toString());
    }

    @Test
    public void ehImutavel() {
        Money original = Money.of("100.00");
        original.plus(Money.of("1.00"));
        original.minus(Money.of("1.00"));
        original.times(2);
        assertEquals(Money.of("100.00"), original);
    }

    @Test
    public void igualdadeEhPorValorNumerico() {
        assertEquals(Money.of("10.0"), Money.of("10.00"));
        assertEquals(Money.of("10.0").hashCode(), Money.of("10.00").hashCode());
        assertNotEquals(Money.of("10.00"), Money.of("10.01"));
    }

    @Test
    public void pisoEmZero() {
        assertSame(Money.ZERO, Money.of("-0.01").atLeastZero());
        assertEquals(Money.of("5.00"), Money.of("5.00").atLeastZero());
        assertTrue(Money.of("-1").isNegative());
        assertFalse(Money.ZERO.isNegative());
    }

    @Test
    public void toBigDecimalSempreTemDuasCasas() {
        assertEquals(new BigDecimal("85.00"), Money.of("85").toBigDecimal());
    }

    @Test
    public void naoAceitaNulo() {
        assertThrows(NullPointerException.class, () -> new Money(null));
    }
}
