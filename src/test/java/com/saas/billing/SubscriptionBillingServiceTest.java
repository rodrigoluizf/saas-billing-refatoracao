package com.saas.billing;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.saas.billing.adapter.payment.LegacyTokenPaymentGateway;
import com.saas.billing.domain.BillingCommand;
import com.saas.billing.domain.Invoice;
import com.saas.billing.domain.InvoiceStatus;
import com.saas.billing.domain.Money;
import com.saas.billing.port.BillingInfrastructureException;
import com.saas.billing.support.FixedClock;
import com.saas.billing.support.InMemoryCustomerRepository;
import com.saas.billing.support.InMemoryInvoiceRepository;
import com.saas.billing.support.RecordingNotificationService;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * Testes de caracterização do fluxo de cobrança: fixam o comportamento observável do
 * código legado (mensagens retornadas, fatura gravada, recibo enviado) usando fakes em
 * memória no lugar de PostgreSQL e SMTP, e um {@link Clock} fixo no lugar de
 * {@code LocalDate.now()}. Rodam no CI sem nenhuma infraestrutura externa.
 */
public class SubscriptionBillingServiceTest {

    private static final String CUSTOMER = "cus_1";
    private static final String EMAIL = "ana@cliente.com";
    private static final Clock OCTOBER_15 = FixedClock.on(2026, 10, 15);
    private static final Clock NOVEMBER_27 = FixedClock.on(2026, 11, 27);

    private InMemoryCustomerRepository customers;
    private InMemoryInvoiceRepository invoices;
    private RecordingNotificationService notifications;

    @Before
    public void setUp() {
        customers = new InMemoryCustomerRepository().with(CUSTOMER, EMAIL, "ACTIVE");
        invoices = new InMemoryInvoiceRepository();
        notifications = new RecordingNotificationService();
    }

    private SubscriptionBillingService serviceOn(Clock clock) {
        return new SubscriptionBillingService(customers, invoices, new LegacyTokenPaymentGateway(),
                notifications, clock);
    }

    private static BillingCommand.Builder command(String basePrice, LocalDate signupDate) {
        return BillingCommand.builder()
                .customerId(CUSTOMER)
                .basePrice(Money.of(basePrice))
                .signupDate(signupDate)
                .paymentCardToken("tok_visa");
    }

    /** Executa em 15/10/2026 e devolve o valor que foi gravado na fatura. */
    private Money chargedOn(Clock clock, BillingCommand cmd) {
        serviceOn(clock).processMonthlyBilling(cmd);
        assertEquals(1, invoices.saved().size());
        return invoices.saved().get(0).amount();
    }

    // ---------------------------------------------------------------- fluxo feliz e falhas de pagamento

    @Test
    public void clienteAtivoComTokenValido_cobraGravaFaturaPagaEEnviaRecibo() {
        List<String> logs = serviceOn(OCTOBER_15)
                .processMonthlyBilling(command("100.00", LocalDate.of(2026, 6, 1)).build());

        assertEquals(asList(
                "Pagamento processado com sucesso: R$ 100.00",
                "E-mail disparado para: ana@cliente.com"), logs);
        assertEquals(asList(new Invoice(CUSTOMER, Money.of("100.00"), InvoiceStatus.PAID)), invoices.saved());
        assertEquals(asList(new RecordingNotificationService.Sent(EMAIL, Money.of("100.00"))), notifications.sent());
    }

    @Test
    public void tokenSemPrefixoTok_registraFalhaMasGravaFaturaFailedEEnviaRecibo() {
        List<String> logs = serviceOn(OCTOBER_15)
                .processMonthlyBilling(command("100.00", LocalDate.of(2026, 6, 1)).paymentCardToken("card_123").build());

        assertEquals(asList(
                "Falha no pagamento do cartão para: cus_1",
                "E-mail disparado para: ana@cliente.com"), logs);
        assertEquals(InvoiceStatus.FAILED, invoices.saved().get(0).status());
        assertEquals(1, notifications.sent().size());
    }

    @Test
    public void tokenNulo_ehFalhaDePagamento() {
        List<String> logs = serviceOn(OCTOBER_15)
                .processMonthlyBilling(command("100.00", LocalDate.of(2026, 6, 1)).paymentCardToken(null).build());

        assertEquals("Falha no pagamento do cartão para: cus_1", logs.get(0));
        assertEquals(InvoiceStatus.FAILED, invoices.saved().get(0).status());
    }

    // ---------------------------------------------------------------- status do cliente

    @Test
    public void clienteInativo_cancelaSemCobrarSemFaturaESemEmail() {
        customers.with(CUSTOMER, EMAIL, "SUSPENDED");

        List<String> logs = serviceOn(OCTOBER_15)
                .processMonthlyBilling(command("100.00", LocalDate.of(2026, 6, 1)).build());

        assertEquals(asList("Cliente inativo. Cobrança cancelada: cus_1"), logs);
        assertTrue(invoices.saved().isEmpty());
        assertTrue(notifications.sent().isEmpty());
    }

    @Test
    public void statusEhComparadoSemDiferenciarMaiusculas() {
        customers.with(CUSTOMER, EMAIL, "active");

        List<String> logs = serviceOn(OCTOBER_15)
                .processMonthlyBilling(command("100.00", LocalDate.of(2026, 6, 1)).build());

        assertEquals("Pagamento processado com sucesso: R$ 100.00", logs.get(0));
    }

    @Test
    public void statusNulo_ehTratadoComoInativo() {
        customers.with(CUSTOMER, EMAIL, null);

        List<String> logs = serviceOn(OCTOBER_15)
                .processMonthlyBilling(command("100.00", LocalDate.of(2026, 6, 1)).build());

        assertEquals(asList("Cliente inativo. Cobrança cancelada: cus_1"), logs);
    }

    @Test
    public void clienteInexistente_retornaListaVaziaSemEfeitosColaterais() {
        List<String> logs = serviceOn(OCTOBER_15)
                .processMonthlyBilling(command("100.00", LocalDate.of(2026, 6, 1)).customerId("nao_existe").build());

        assertEquals(emptyList(), logs);
        assertTrue(invoices.saved().isEmpty());
        assertTrue(notifications.sent().isEmpty());
    }

    // ---------------------------------------------------------------- regras de preço (via fluxo completo)

    @Test
    public void fidelidade_mais_de_24_meses_da15PorCento() {
        assertEquals(Money.of("85.00"), chargedOn(OCTOBER_15, command("100.00", LocalDate.of(2024, 9, 1)).build()));
    }

    @Test
    public void fidelidade_exatamente_24_meses_daApenas10PorCento() {
        assertEquals(Money.of("90.00"), chargedOn(OCTOBER_15, command("100.00", LocalDate.of(2024, 10, 31)).build()));
    }

    @Test
    public void fidelidade_exatamente_12_meses_naoDaDesconto() {
        assertEquals(Money.of("100.00"), chargedOn(OCTOBER_15, command("100.00", LocalDate.of(2025, 10, 1)).build()));
    }

    @Test
    public void ongTem30PorCentoSobreOPrecoComFidelidade() {
        BillingCommand cmd = command("100.00", LocalDate.of(2024, 9, 1)).nonProfit(true).build();
        assertEquals(Money.of("59.50"), chargedOn(OCTOBER_15, cmd)); // 100 x 0,85 x 0,70
    }

    @Test
    public void blackFridayForaDeNovembro_naoTemEfeito() {
        BillingCommand cmd = command("100.00", LocalDate.of(2026, 6, 1)).promoCode("BLACKFRIDAY").build();
        assertEquals(Money.of("100.00"), chargedOn(OCTOBER_15, cmd));
    }

    @Test
    public void blackFridayEmNovembro_tiraTrintaReaisEIgnoraMaiusculas() {
        BillingCommand cmd = command("100.00", LocalDate.of(2026, 6, 1)).promoCode("blackfriday").build();
        assertEquals(Money.of("70.00"), chargedOn(NOVEMBER_27, cmd));
    }

    @Test
    public void cupomComEspacosNaoEhReconhecido_comportamentoLegadoPreservado() {
        BillingCommand cmd = command("100.00", LocalDate.of(2026, 6, 1)).promoCode(" BLACKFRIDAY ").build();
        assertEquals(Money.of("100.00"), chargedOn(NOVEMBER_27, cmd));
    }

    @Test
    public void welcome10_valeAteUmMesDeAdesao() {
        assertEquals(Money.of("90.00"),
                chargedOn(OCTOBER_15, command("100.00", LocalDate.of(2026, 9, 30)).promoCode("WELCOME10").build()));
    }

    @Test
    public void welcome10_naoValeComDoisMeses() {
        assertEquals(Money.of("100.00"),
                chargedOn(OCTOBER_15, command("100.00", LocalDate.of(2026, 8, 1)).promoCode("WELCOME10").build()));
    }

    @Test
    public void atraso_aplicaMultaDe2PorCentoMaisJurosDe0v1PorCentoAoDia() {
        BillingCommand cmd = command("100.00", LocalDate.of(2026, 6, 1)).overdueDays(10).build();
        assertEquals(Money.of("103.00"), chargedOn(OCTOBER_15, cmd)); // 100 + 2 + 1
    }

    @Test
    public void precoNegativoViraZeroMesmoComAtraso() {
        BillingCommand cmd = command("20.00", LocalDate.of(2026, 6, 1)).promoCode("BLACKFRIDAY").overdueDays(5).build();
        assertEquals(Money.ZERO, chargedOn(NOVEMBER_27, cmd));
    }

    @Test
    public void valoresSaoArredondadosACentavos() {
        // Legado em double: 99.99 x 0,85 x 0,70 = 59.494049999999994 (aparecia assim no log e no e-mail)
        BillingCommand cmd = command("99.99", LocalDate.of(2024, 9, 1)).nonProfit(true).build();

        List<String> logs = serviceOn(OCTOBER_15).processMonthlyBilling(cmd);

        assertEquals("Pagamento processado com sucesso: R$ 59.49", logs.get(0));
    }

    @Test
    public void mesmoComandoEMesmoRelogio_mesmoResultado() {
        BillingCommand cmd = command("100.00", LocalDate.of(2025, 1, 1)).promoCode("BLACKFRIDAY").overdueDays(3).build();
        List<String> first = serviceOn(NOVEMBER_27).processMonthlyBilling(cmd);
        List<String> second = serviceOn(NOVEMBER_27).processMonthlyBilling(cmd);
        assertEquals(first, second);
    }

    // ---------------------------------------------------------------- falhas de infraestrutura

    @Test
    public void bancoIndisponivel_retornaErroFatalComAMensagemOriginal() {
        customers.failWith(BillingInfrastructureException.wrapping(new SQLException("Connection refused")));

        List<String> logs = serviceOn(OCTOBER_15)
                .processMonthlyBilling(command("100.00", LocalDate.of(2026, 6, 1)).build());

        assertEquals(asList("Erro fatal no processamento: Connection refused"), logs);
    }

    @Test
    public void falhaAoGravarFatura_mantemLogDoPagamentoENaoEnviaRecibo() {
        invoices.failWith(BillingInfrastructureException.wrapping(new SQLException("disk full")));

        List<String> logs = serviceOn(OCTOBER_15)
                .processMonthlyBilling(command("100.00", LocalDate.of(2026, 6, 1)).build());

        assertEquals(asList(
                "Pagamento processado com sucesso: R$ 100.00",
                "Erro fatal no processamento: disk full"), logs);
        assertTrue(notifications.sent().isEmpty());
    }

    @Test
    public void falhaNoSmtp_faturaJaGravadaELogDeEmailNaoAparece() {
        notifications.failWith(BillingInfrastructureException.wrapping(new RuntimeException("smtp down")));

        List<String> logs = serviceOn(OCTOBER_15)
                .processMonthlyBilling(command("100.00", LocalDate.of(2026, 6, 1)).build());

        assertEquals(asList(
                "Pagamento processado com sucesso: R$ 100.00",
                "Erro fatal no processamento: smtp down"), logs);
        assertEquals(1, invoices.saved().size());
    }

    @Test
    public void erroDeProgramacaoNaoEhMaisMascarado() {
        customers.failWith(new IllegalStateException("bug"));

        BillingCommand cmd = command("100.00", LocalDate.of(2026, 6, 1)).build();
        assertThrows(IllegalStateException.class, () -> serviceOn(OCTOBER_15).processMonthlyBilling(cmd));
    }

    // ---------------------------------------------------------------- assinatura legada

    @Test
    @SuppressWarnings("deprecation")
    public void assinaturaLegadaContinuaFuncionandoEIgnoraPlanType() {
        List<String> logs = serviceOn(OCTOBER_15).processMonthlyBilling(
                CUSTOMER, "QUALQUER_PLANO", 100.0, "2024-09-01", 0, null, false, "tok_visa");

        assertEquals(asList(
                "Pagamento processado com sucesso: R$ 85.00",
                "E-mail disparado para: ana@cliente.com"), logs);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void assinaturaLegada_dataInvalidaContinuaLancandoExcecao() {
        SubscriptionBillingService service = serviceOn(OCTOBER_15);
        assertThrows(DateTimeParseException.class, () -> service.processMonthlyBilling(
                CUSTOMER, "PRO", 100.0, "01/09/2024", 0, null, false, "tok_visa"));
    }
}
