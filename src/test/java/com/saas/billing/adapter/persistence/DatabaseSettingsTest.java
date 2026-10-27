package com.saas.billing.adapter.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.util.Map;
import org.junit.Test;

public class DatabaseSettingsTest {

    @Test
    public void senhaEhObrigatoria() {
        assertThrows(IllegalStateException.class, () -> DatabaseSettings.fromEnvironment(Map.of()));
        assertThrows(IllegalStateException.class,
                () -> DatabaseSettings.fromEnvironment(Map.of("BILLING_DB_PASSWORD", " ")));
    }

    @Test
    public void urlEUsuarioTemOsPadroesDoLegado() {
        DatabaseSettings settings = DatabaseSettings.fromEnvironment(Map.of("BILLING_DB_PASSWORD", "s3cr3t"));

        assertEquals("jdbc:postgresql://localhost:5432/saas_db", settings.url());
        assertEquals("admin", settings.user());
        assertEquals("s3cr3t", settings.password());
    }

    @Test
    public void valoresDoAmbienteTemPrioridade() {
        DatabaseSettings settings = DatabaseSettings.fromEnvironment(Map.of(
                "BILLING_DB_URL", "jdbc:postgresql://db:5432/billing",
                "BILLING_DB_USER", "billing_app",
                "BILLING_DB_PASSWORD", "s3cr3t"));

        assertEquals("jdbc:postgresql://db:5432/billing", settings.url());
        assertEquals("billing_app", settings.user());
    }

    @Test
    public void toStringNaoVazaASenha() {
        DatabaseSettings settings = new DatabaseSettings("jdbc:x", "u", "s3cr3t");
        assertFalse(settings.toString().contains("s3cr3t"));
    }
}
