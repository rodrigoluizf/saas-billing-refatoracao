package com.saas.billing.port;

import com.saas.billing.domain.Customer;
import java.util.Optional;

/** Porta de saída: consulta de clientes. */
public interface CustomerRepository {

    /**
     * @return o cliente, ou vazio se não existir
     * @throws BillingInfrastructureException se o armazenamento estiver indisponível
     */
    Optional<Customer> findById(String customerId);
}
