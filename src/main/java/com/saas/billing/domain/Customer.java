package com.saas.billing.domain;

/**
 * Cliente da plataforma, como lido pela porta {@link com.saas.billing.port.CustomerRepository}.
 *
 * <p>A regra de "cliente ativo" do legado foi preservada: o status é comparado com
 * {@code "ACTIVE"} sem diferenciar maiúsculas/minúsculas, e status nulo conta como inativo.
 */
public record Customer(String id, String email, String status) {

    private static final String ACTIVE_STATUS = "ACTIVE";

    public boolean isActive() {
        return ACTIVE_STATUS.equalsIgnoreCase(status);
    }
}
