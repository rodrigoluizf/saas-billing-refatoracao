package com.saas.billing.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * Golden master: compara o {@link PricingCalculator} com o algoritmo legado
 * ({@link LegacyPricingOracle}) em dezenas de milhares de combinações de preço, data de
 * adesão, data de referência, cupom, ONG e dias de atraso.
 *
 * <p>Critério: o valor novo tem que ser igual ao valor legado levado a centavos
 * (HALF_EVEN). A única tolerância aceita é o caso em que o valor exato termina em meio
 * centavo (ex.: 91,225) — aí o {@code double} do legado cai ora um pouco acima, ora um
 * pouco abaixo do empate, por erro de representação, e o arredondamento dele é aleatório.
 * Nesses casos o novo cálculo é o correto, e a diferença máxima é de 1 centavo.
 */
public class PricingGoldenMasterTest {

    private static final double[] BASE_PRICES = {
        0.01, 9.90, 19.99, 29.90, 30.00, 49.90, 59.99, 99.99, 100.00, 149.90, 199.99, 333.33, 1234.56};
    private static final int[] MONTHS_BEFORE_TODAY = {-3, -1, 0, 1, 2, 11, 12, 13, 23, 24, 25, 40};
    private static final int[] SIGNUP_DAYS = {1, 28};
    private static final LocalDate[] TODAYS = {
        LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), LocalDate.of(2027, 1, 5)};
    private static final int[] OVERDUE_DAYS = {-1, 0, 1, 5, 7, 30, 90};
    private static final String[] PROMO_CODES = {
        null, "", "  ", "BLACKFRIDAY", "blackfriday", " BLACKFRIDAY", "WELCOME10", "Welcome10", "XPTO"};
    private static final boolean[] NON_PROFIT = {false, true};

    private static final BigDecimal HALF_CENT = new BigDecimal("0.005");
    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    public void calculadoraReproduzOAlgoritmoLegado() {
        int total = 0;
        int exact = 0;
        List<String> realDivergences = new ArrayList<>();

        for (LocalDate today : TODAYS) {
            for (int monthsBefore : MONTHS_BEFORE_TODAY) {
                for (int day : SIGNUP_DAYS) {
                    LocalDate signup = today.withDayOfMonth(1).minusMonths(monthsBefore).withDayOfMonth(day);
                    for (double base : BASE_PRICES) {
                        for (int overdue : OVERDUE_DAYS) {
                            for (String promo : PROMO_CODES) {
                                for (boolean nonProfit : NON_PROFIT) {
                                    total++;
                                    double legacy = LegacyPricingOracle.legacyPrice(
                                            base, signup.toString(), overdue, promo, nonProfit, today);
                                    BigDecimal legacyCents = BigDecimal.valueOf(legacy).setScale(2, RoundingMode.HALF_EVEN);

                                    BillingCommand cmd = BillingCommand.builder().customerId("c")
                                            .basePrice(Money.of(base)).signupDate(signup).overdueDays(overdue)
                                            .promoCode(promo).nonProfit(nonProfit).build();
                                    BigDecimal actual = calculator.calculate(cmd, today).toBigDecimal();

                                    if (actual.compareTo(legacyCents) == 0) {
                                        exact++;
                                    } else if (!isHalfCentTie(legacy, actual, legacyCents)) {
                                        realDivergences.add(String.format("base=%s signup=%s today=%s overdue=%d promo=%s ong=%s"
                                                + " -> legado=%s (%s) novo=%s", base, signup, today, overdue, promo,
                                                nonProfit, legacy, legacyCents, actual));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        assertTrue("Divergências reais (" + realDivergences.size() + "):\n"
                + String.join("\n", realDivergences.subList(0, Math.min(20, realDivergences.size()))),
                realDivergences.isEmpty());
        assertEquals(TODAYS.length * MONTHS_BEFORE_TODAY.length * SIGNUP_DAYS.length * BASE_PRICES.length
                * OVERDUE_DAYS.length * PROMO_CODES.length * NON_PROFIT.length, total);
        // Imprime o placar para o relatório (aparece na saída do Maven/Surefire).
        System.out.printf("Golden master: %d combinações, %d idênticas ao legado em centavos, %d empates de meio centavo%n",
                total, exact, total - exact);
    }

    /** Diferença de 1 centavo explicada por um valor legado colado no meio centavo. */
    private static boolean isHalfCentTie(double legacy, BigDecimal actual, BigDecimal legacyCents) {
        BigDecimal distanceToTie = new BigDecimal(legacy).subtract(legacyCents).abs().subtract(HALF_CENT).abs();
        return actual.subtract(legacyCents).abs().compareTo(ONE_CENT) == 0
                && distanceToTie.compareTo(new BigDecimal("1E-9")) < 0;
    }

    @Test
    public void gradeCobreTodosOsRamos() {
        assertTrue(Arrays.stream(TODAYS).anyMatch(d -> d.getMonthValue() == 11));
        assertTrue(Arrays.stream(TODAYS).anyMatch(d -> d.getMonthValue() != 11));
        assertTrue(Arrays.stream(MONTHS_BEFORE_TODAY).anyMatch(m -> m > 24));
        assertTrue(Arrays.stream(MONTHS_BEFORE_TODAY).anyMatch(m -> m > 12 && m <= 24));
        assertTrue(Arrays.stream(MONTHS_BEFORE_TODAY).anyMatch(m -> m <= 1));
        assertTrue(Arrays.stream(OVERDUE_DAYS).anyMatch(d -> d > 0));
    }
}
