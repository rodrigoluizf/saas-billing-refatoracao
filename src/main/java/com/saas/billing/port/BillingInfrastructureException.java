package com.saas.billing.port;

/**
 * Falha de infraestrutura (banco, gateway, SMTP) traduzida pelos adaptadores.
 *
 * <p>É a única exceção que o serviço de cobrança trata. Ela substitui o antigo
 * catch genérico de {@code Exception}, que também engolia erros de programação. A mensagem
 * da causa original é preservada, para manter o texto exibido nos logs de cobrança.
 */
public class BillingInfrastructureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BillingInfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Envolve a exceção técnica mantendo exatamente a mensagem original. */
    public static BillingInfrastructureException wrapping(Throwable cause) {
        return new BillingInfrastructureException(cause.getMessage(), cause);
    }
}
