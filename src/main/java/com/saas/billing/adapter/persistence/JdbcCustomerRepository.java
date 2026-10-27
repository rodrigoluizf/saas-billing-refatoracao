package com.saas.billing.adapter.persistence;

import com.saas.billing.domain.Customer;
import com.saas.billing.port.BillingInfrastructureException;
import com.saas.billing.port.CustomerRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/** Adaptador JDBC (PostgreSQL) da porta {@link CustomerRepository}. Mesma consulta do legado. */
public class JdbcCustomerRepository implements CustomerRepository {

    private static final String FIND_BY_ID = "SELECT email, status FROM customers WHERE id = ?";

    private final ConnectionFactory connectionFactory;

    public JdbcCustomerRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        try (Connection conn = connectionFactory.open();
             PreparedStatement stmt = conn.prepareStatement(FIND_BY_ID)) {
            stmt.setString(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Customer(customerId, rs.getString("email"), rs.getString("status")));
            }
        } catch (SQLException e) {
            throw BillingInfrastructureException.wrapping(e);
        }
    }
}
