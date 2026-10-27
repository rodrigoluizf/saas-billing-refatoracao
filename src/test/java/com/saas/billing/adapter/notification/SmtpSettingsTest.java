package com.saas.billing.adapter.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.saas.billing.domain.Money;
import java.util.Map;
import org.junit.Test;

public class SmtpSettingsTest {

    @Test
    public void padroesSaoOsDoLegadoComEnvioDesligado() {
        SmtpSettings settings = SmtpSettings.fromEnvironment(Map.of());

        assertEquals(SmtpSettings.legacyDefaults(), settings);
        assertEquals("smtp.empresa.com", settings.host());
        assertEquals(587, settings.port());
        assertEquals("billing@empresa.com", settings.from());
        assertFalse(settings.deliveryEnabled());
    }

    @Test
    public void leituraDoAmbiente() {
        SmtpSettings settings = SmtpSettings.fromEnvironment(Map.of(
                "BILLING_SMTP_HOST", "mail.local",
                "BILLING_SMTP_PORT", "2525",
                "BILLING_SMTP_FROM", "nao-responda@saas.com",
                "BILLING_SMTP_DELIVERY_ENABLED", "true"));

        assertEquals(new SmtpSettings("mail.local", 2525, "nao-responda@saas.com", true), settings);
        assertTrue(settings.deliveryEnabled());
    }

    @Test
    public void comEnvioDesligado_montaOEmailSemAbrirConexaoSmtp() {
        new SmtpNotificationService(SmtpSettings.legacyDefaults())
                .sendInvoiceReceipt("ana@cliente.com", Money.of("85.00"));
    }
}
