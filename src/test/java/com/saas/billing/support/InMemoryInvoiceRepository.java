package com.saas.billing.support;

import com.saas.billing.domain.Invoice;
import com.saas.billing.port.InvoiceRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryInvoiceRepository implements InvoiceRepository {

    private final List<Invoice> saved = new ArrayList<>();
    private RuntimeException failure;

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public List<Invoice> saved() {
        return Collections.unmodifiableList(saved);
    }

    @Override
    public void save(Invoice invoice) {
        if (failure != null) {
            throw failure;
        }
        saved.add(invoice);
    }
}
