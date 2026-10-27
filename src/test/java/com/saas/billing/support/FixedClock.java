package com.saas.billing.support;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Relógios fixos para testes determinísticos. */
public final class FixedClock {

    public static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private FixedClock() {
    }

    public static Clock on(int year, int month, int day) {
        return on(LocalDate.of(year, month, day));
    }

    public static Clock on(LocalDate date) {
        return Clock.fixed(date.atTime(12, 0).atZone(SAO_PAULO).toInstant(), SAO_PAULO);
    }
}
