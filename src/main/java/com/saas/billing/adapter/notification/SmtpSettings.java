package com.saas.billing.adapter.notification;

import java.util.Map;

/**
 * Configuração SMTP, lida do ambiente com os mesmos padrões do legado.
 *
 * <p>{@code deliveryEnabled} existe porque o legado <b>montava o e-mail mas nunca enviava</b>
 * ({@code emailSender.send()} estava comentado). Para não mudar esse comportamento em
 * produção sem decisão de negócio, o envio real vem desligado por padrão e é ligado com
 * {@code BILLING_SMTP_DELIVERY_ENABLED=true}.
 */
public record SmtpSettings(String host, int port, String from, boolean deliveryEnabled) {

    static final String DEFAULT_HOST = "smtp.empresa.com";
    static final int DEFAULT_PORT = 587;
    static final String DEFAULT_FROM = "billing@empresa.com";

    public static SmtpSettings legacyDefaults() {
        return new SmtpSettings(DEFAULT_HOST, DEFAULT_PORT, DEFAULT_FROM, false);
    }

    public static SmtpSettings fromEnvironment(Map<String, String> env) {
        return new SmtpSettings(
                env.getOrDefault("BILLING_SMTP_HOST", DEFAULT_HOST),
                Integer.parseInt(env.getOrDefault("BILLING_SMTP_PORT", String.valueOf(DEFAULT_PORT))),
                env.getOrDefault("BILLING_SMTP_FROM", DEFAULT_FROM),
                Boolean.parseBoolean(env.getOrDefault("BILLING_SMTP_DELIVERY_ENABLED", "false")));
    }
}
