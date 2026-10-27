package com.saas.billing.adapter.persistence;

import com.saas.billing.domain.Invoice;
import com.saas.billing.port.BillingInfrastructureException;
import com.saas.billing.port.InvoiceRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

/** Adaptador JDBC (PostgreSQL) da porta {@link InvoiceRepository}. Mesmo INSERT do legado. */
public class JdbcInvoiceRepository implements InvoiceRepository {

    private static final String INSERT_INVOICE =
            "INSERT INTO invoices(customer_id, amount, status, created_at) VALUES (?, ?, ?, NOW())";

    private final ConnectionFactory connectionFactory;

    public JdbcInvoiceRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public void save(Invoice invoice) {
        try (Connection conn = connectionFactory.open();
             PreparedStatement stmt = conn.prepareStatement(INSERT_INVOICE)) {
            stmt.setString(1, invoice.customerId());
            stmt.setBigDecimal(2, invoice.amount().toBigDecimal());
            stmt.setString(3, invoice.status().name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw BillingInfrastructureException.wrapping(e);
        }
    }
}
