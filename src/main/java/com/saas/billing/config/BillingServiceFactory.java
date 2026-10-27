package com.saas.billing.config;

import com.saas.billing.SubscriptionBillingService;
import com.saas.billing.adapter.notification.SmtpNotificationService;
import com.saas.billing.adapter.notification.SmtpSettings;
import com.saas.billing.adapter.payment.LegacyTokenPaymentGateway;
import com.saas.billing.adapter.persistence.ConnectionFactory;
import com.saas.billing.adapter.persistence.DatabaseSettings;
import com.saas.billing.adapter.persistence.JdbcCustomerRepository;
import com.saas.billing.adapter.persistence.JdbcInvoiceRepository;
import java.time.Clock;

/**
 * Composition root: o único lugar que conhece os adaptadores concretos.
 * Substitui o antigo {@code new SubscriptionBillingService()} dos chamadores.
 */
public final class BillingServiceFactory {

    private BillingServiceFactory() {
    }

    /** Produção: configuração vinda de variáveis de ambiente e relógio do sistema. */
    public static SubscriptionBillingService createDefault() {
        return create(
                DatabaseSettings.fromEnvironment(System.getenv()),
                SmtpSettings.fromEnvironment(System.getenv()),
                Clock.systemDefaultZone());
    }

    public static SubscriptionBillingService create(DatabaseSettings database, SmtpSettings smtp, Clock clock) {
        ConnectionFactory connections = ConnectionFactory.driverManager(database);
        return new SubscriptionBillingService(
                new JdbcCustomerRepository(connections),
                new JdbcInvoiceRepository(connections),
                new LegacyTokenPaymentGateway(),
                new SmtpNotificationService(smtp),
                clock);
    }
}
