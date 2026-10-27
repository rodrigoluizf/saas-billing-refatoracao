package com.saas.billing.adapter.persistence;

import java.util.Map;
import java.util.Objects;

/**
 * Configuração de acesso ao PostgreSQL, lida do ambiente em vez de ficar fixa no código.
 *
 * <p>URL e usuário têm como padrão os valores que o legado usava; a senha não tem padrão
 * e precisa vir de {@code BILLING_DB_PASSWORD} (segredo não fica mais versionado).
 */
public record DatabaseSettings(String url, String user, String password) {

    public static final String URL_VARIABLE = "BILLING_DB_URL";
    public static final String USER_VARIABLE = "BILLING_DB_USER";
    public static final String PASSWORD_VARIABLE = "BILLING_DB_PASSWORD";

    static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/saas_db";
    static final String DEFAULT_USER = "admin";

    public DatabaseSettings {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(password, "password");
    }

    public static DatabaseSettings fromEnvironment(Map<String, String> env) {
        String password = env.get(PASSWORD_VARIABLE);
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Variável de ambiente " + PASSWORD_VARIABLE + " não definida");
        }
        return new DatabaseSettings(
                env.getOrDefault(URL_VARIABLE, DEFAULT_URL),
                env.getOrDefault(USER_VARIABLE, DEFAULT_USER),
                password);
    }

    /** Não expõe a senha em logs. */
    @Override
    public String toString() {
        return "DatabaseSettings[url=" + url + ", user=" + user + ", password=***]";
    }
}
