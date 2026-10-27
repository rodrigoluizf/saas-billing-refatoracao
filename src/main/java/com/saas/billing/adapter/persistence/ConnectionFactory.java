package com.saas.billing.adapter.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Abre conexões JDBC. Permite trocar {@link DriverManager} por um pool sem mexer nos repositórios. */
@FunctionalInterface
public interface ConnectionFactory {

    Connection open() throws SQLException;

    static ConnectionFactory driverManager(DatabaseSettings settings) {
        return () -> DriverManager.getConnection(settings.url(), settings.user(), settings.password());
    }
}
