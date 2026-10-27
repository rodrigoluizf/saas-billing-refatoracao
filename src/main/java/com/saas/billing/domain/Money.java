package com.saas.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object imutável que representa uma quantia em reais.
 *
 * <p>Substitui o {@code double} usado pelo código legado (Primitive Obsession). As operações
 * aritméticas são exatas ({@link BigDecimal} não perde precisão em somas, subtrações e
 * multiplicações por fatores decimais), e o arredondamento acontece <b>uma única vez</b>, de forma
 * explícita, em {@link #rounded()} — sempre com {@value #SCALE} casas e
 * {@link RoundingMode#HALF_EVEN} (arredondamento bancário, ABNT NBR 5891).
 *
 * <p>Arredondar só no final evita o erro acumulado que surgiria ao arredondar cada etapa
 * intermediária do cálculo e mantém o valor final idêntico ao do algoritmo legado
 * quando este é levado a centavos.
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount não pode ser nulo");
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    /**
     * Ponte com a API legada, que recebia o preço como {@code double}. Usa a representação
     * decimal mais curta do double (ex.: 99.99 vira exatamente 99.99, não 99.9899999...).
     */
    public static Money of(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("Valor monetário inválido: " + amount);
        }
        return new Money(BigDecimal.valueOf(amount));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(factor));
    }

    public Money times(long factor) {
        return times(BigDecimal.valueOf(factor));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    /** Piso em zero: valores negativos viram {@link #ZERO}. */
    public Money atLeastZero() {
        return isNegative() ? ZERO : this;
    }

    /** Valor arredondado a centavos ({@value #SCALE} casas, HALF_EVEN). */
    public Money rounded() {
        return new Money(amount.setScale(SCALE, ROUNDING_MODE));
    }

    /** Valor em centavos como {@link BigDecimal}, pronto para persistência/integração. */
    public BigDecimal toBigDecimal() {
        return amount.setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    /** Igualdade por valor numérico: {@code 10.0} e {@code 10.00} são o mesmo dinheiro. */
    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Money other && amount.compareTo(other.amount) == 0);
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    /** Representação em centavos, ex.: {@code 85.00}. */
    @Override
    public String toString() {
        return toBigDecimal().toPlainString();
    }
}
