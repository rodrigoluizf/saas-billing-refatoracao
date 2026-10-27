package com.saas.billing.adapter.notification;

import com.saas.billing.domain.Money;
import com.saas.billing.port.BillingInfrastructureException;
import com.saas.billing.port.NotificationService;
import java.util.Objects;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

/** Adaptador SMTP (Apache Commons Email) da porta {@link NotificationService}. */
public class SmtpNotificationService implements NotificationService {

    static final String SUBJECT = "Sua Fatura Mensal";
    static final String MESSAGE_PREFIX = "Olá! Sua fatura foi processada no valor de R$ ";

    private final SmtpSettings settings;

    public SmtpNotificationService(SmtpSettings settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    @Override
    public void sendInvoiceReceipt(String email, Money amount) {
        try {
            SimpleEmail message = new SimpleEmail();
            message.setHostName(settings.host());
            message.setSmtpPort(settings.port());
            message.setFrom(settings.from());
            message.setSubject(SUBJECT);
            message.setMsg(MESSAGE_PREFIX + amount);
            message.addTo(email);
            if (settings.deliveryEnabled()) {
                message.send();
            }
        } catch (EmailException e) {
            throw BillingInfrastructureException.wrapping(e);
        }
    }
}
