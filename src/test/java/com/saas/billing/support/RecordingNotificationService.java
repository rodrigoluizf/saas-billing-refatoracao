package com.saas.billing.support;

import com.saas.billing.domain.Money;
import com.saas.billing.port.NotificationService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Fake que só registra os recibos "enviados": substitui o servidor SMTP nos testes. */
public class RecordingNotificationService implements NotificationService {

    public record Sent(String email, Money amount) {
    }

    private final List<Sent> sent = new ArrayList<>();
    private RuntimeException failure;

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public List<Sent> sent() {
        return Collections.unmodifiableList(sent);
    }

    @Override
    public void sendInvoiceReceipt(String email, Money amount) {
        if (failure != null) {
            throw failure;
        }
        sent.add(new Sent(email, amount));
    }
}
