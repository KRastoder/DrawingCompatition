package com.drawing_compatition.KRastoder.drawing;

import com.drawing_compatition.KRastoder.common.BadRequestException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public record DateRange(Instant from, Instant to) {

    private static final Instant UNBOUNDED_START = Instant.EPOCH;
    private static final Instant UNBOUNDED_END = Instant.parse("9999-12-31T23:59:59Z");

    public static DateRange of(LocalDate date, LocalDate from, LocalDate to) {
        if (date != null && (from != null || to != null)) {
            throw new BadRequestException("Use either a single date or a date range, not both.");
        }

        if ((from == null) != (to == null)) {
            throw new BadRequestException("Both from and to are required for a date range.");
        }

        if (date != null) {
            return new DateRange(
                    date.atStartOfDay(ZoneOffset.UTC).toInstant(),
                    date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
            );
        }

        if (from != null) {
            if (from.isAfter(to)) {
                throw new BadRequestException("The start date must be on or before the end date.");
            }
            return new DateRange(
                    from.atStartOfDay(ZoneOffset.UTC).toInstant(),
                    to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
            );
        }

        return new DateRange(UNBOUNDED_START, UNBOUNDED_END);
    }
}
