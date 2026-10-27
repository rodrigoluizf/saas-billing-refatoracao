package com.saas.billing.domain;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import org.junit.Test;

/** Testes unitários das regras de preço, sem nenhum fake: a classe é pura. */
public class PricingCalculatorTest {

    private static final LocalDate OCTOBER_15 = LocalDate.of(2026, 10, 15);
    private static final LocalDate NOVEMBER_27 = LocalDate.of(2026, 11, 27);
    private static final LocalDate RECENT_SIGNUP = LocalDate.of(2026, 6, 1); // 4 meses: sem fidelidade

    private final PricingCalculator calculator = new PricingCalculator();

    private static BillingCommand.Builder price(String basePrice) {
        return BillingCommand.builder().customerId("c").basePrice(Money.of(basePrice)).signupDate(RECENT_SIGNUP);
    }

    private Money calc(BillingCommand.Builder builder, LocalDate today) {
        return calculator.calculate(builder.build(), today);
    }

    // ------------------------------------------------------------ meses de casa

    @Test
    public void mesesDeCasaIgnoramODia() {
        assertEquals(1, PricingCalculator.monthsActive(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 1)));
        assertEquals(0, PricingCalculator.monthsActive(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
        assertEquals(25, PricingCalculator.monthsActive(LocalDate.of(2024, 9, 28), OCTOBER_15));
    }

    @Test
    public void mesesDeCasaPodemSerNegativosParaAdesaoFutura() {
        assertEquals(-2, PricingCalculator.monthsActive(LocalDate.of(2026, 12, 1), OCTOBER_15));
    }

    // ------------------------------------------------------------ fidelidade

    @Test
    public void semFidelidadeAte12Meses() {
        assertEquals(Money.of("100.00"), calc(price("100").signupDate(LocalDate.of(2025, 10, 1)), OCTOBER_15));
    }

    @Test
    public void dezPorCentoDe13a24Meses() {
        assertEquals(Money.of("90.00"), calc(price("100").signupDate(LocalDate.of(2025, 9, 1)), OCTOBER_15));
        assertEquals(Money.of("90.00"), calc(price("100").signupDate(LocalDate.of(2024, 10, 1)), OCTOBER_15));
    }

    @Test
    public void quinzePorCentoAcimaDe24Meses() {
        assertEquals(Money.of("85.00"), calc(price("100").signupDate(LocalDate.of(2024, 9, 1)), OCTOBER_15));
    }

    // ------------------------------------------------------------ ONG

    @Test
    public void ongPagaSetentaPorCento() {
        assertEquals(Money.of("70.00"), calc(price("100").nonProfit(true), OCTOBER_15));
    }

    // ------------------------------------------------------------ cupons

    @Test
    public void blackFridaySoEmNovembro() {
        assertEquals(Money.of("70.00"), calc(price("100").promoCode("BLACKFRIDAY"), NOVEMBER_27));
        assertEquals(Money.of("100.00"), calc(price("100").promoCode("BLACKFRIDAY"), OCTOBER_15));
    }

    @Test
    public void blackFridayEhAplicadoDepoisDoDescontoDeOng() {
        // 100 x 0,70 = 70 ; 70 - 30 = 40  (e não (100 - 30) x 0,70 = 49)
        assertEquals(Money.of("40.00"), calc(price("100").nonProfit(true).promoCode("BLACKFRIDAY"), NOVEMBER_27));
    }

    @Test
    public void welcome10AteUmMes() {
        assertEquals(Money.of("90.00"),
                calc(price("100").signupDate(LocalDate.of(2026, 9, 1)).promoCode("welcome10"), OCTOBER_15));
        assertEquals(Money.of("100.00"),
                calc(price("100").signupDate(LocalDate.of(2026, 8, 1)).promoCode("WELCOME10"), OCTOBER_15));
    }

    @Test
    public void welcome10ValeParaAdesaoFutura() {
        assertEquals(Money.of("90.00"),
                calc(price("100").signupDate(LocalDate.of(2027, 1, 1)).promoCode("WELCOME10"), OCTOBER_15));
    }

    @Test
    public void cupomDesconhecidoBrancoOuComEspacosNaoTemEfeito() {
        assertEquals(Money.of("100.00"), calc(price("100").promoCode("XPTO"), NOVEMBER_27));
        assertEquals(Money.of("100.00"), calc(price("100").promoCode("   "), NOVEMBER_27));
        assertEquals(Money.of("100.00"), calc(price("100").promoCode(" BLACKFRIDAY"), NOVEMBER_27));
    }

    // ------------------------------------------------------------ atraso

    @Test
    public void atrasoSobreOPrecoJaComDescontos() {
        // 100 x 0,85 = 85 ; multa 1,70 ; juros 85 x 0,001 x 10 = 0,85 ; total 87,55
        assertEquals(Money.of("87.55"),
                calc(price("100").signupDate(LocalDate.of(2024, 1, 1)).overdueDays(10), OCTOBER_15));
    }

    @Test
    public void diasDeAtrasoZeroOuNegativosNaoGeramEncargos() {
        assertEquals(Money.of("100.00"), calc(price("100").overdueDays(0), OCTOBER_15));
        assertEquals(Money.of("100.00"), calc(price("100").overdueDays(-3), OCTOBER_15));
    }

    // ------------------------------------------------------------ piso e arredondamento

    @Test
    public void nuncaCobraValorNegativo() {
        assertEquals(Money.ZERO, calc(price("19.90").promoCode("BLACKFRIDAY").overdueDays(30), NOVEMBER_27));
    }

    @Test
    public void arredondaUmaVezSoNoFinal() {
        // 99,99 x 0,85 x 0,70 = 59,49405 -> 59,49
        assertEquals(Money.of("59.49"),
                calc(price("99.99").signupDate(LocalDate.of(2020, 1, 1)).nonProfit(true), OCTOBER_15));
    }

    @Test
    public void todasAsRegrasJuntas() {
        // 200 x 0,85 = 170 ; x 0,70 = 119 ; - 30 = 89 ; + 2% (1,78) + 0,1% x 5 (0,445) = 91,225 -> 91,22 (HALF_EVEN)
        assertEquals(Money.of("91.22"), calc(price("200").signupDate(LocalDate.of(2023, 1, 10))
                .nonProfit(true).promoCode("BLACKFRIDAY").overdueDays(5), NOVEMBER_27));
    }
}
