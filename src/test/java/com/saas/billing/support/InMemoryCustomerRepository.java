package com.saas.billing.support;

import com.saas.billing.domain.Customer;
import com.saas.billing.port.CustomerRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Fake em memória: substitui o PostgreSQL nos testes (CI sem infraestrutura externa). */
public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<String, Customer> customers = new HashMap<>();
    private RuntimeException failure;

    public InMemoryCustomerRepository with(String id, String email, String status) {
        customers.put(id, new Customer(id, email, status));
        return this;
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        if (failure != null) {
            throw failure;
        }
        return Optional.ofNullable(customers.get(customerId));
    }
}
