package com.drawing_compatition.KRastoder.drawing;

import com.drawing_compatition.KRastoder.common.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    @Test
    void singleDayCoversTheWholeUtcDay() {
        DateRange range = DateRange.of(LocalDate.of(2026, 10, 19), null, null);

        assertThat(range.from()).isEqualTo(Instant.parse("2026-10-19T00:00:00Z"));
        assertThat(range.to()).isEqualTo(Instant.parse("2026-10-20T00:00:00Z"));
    }

    @Test
    void rangeIsInclusiveOfTheEndDate() {
        DateRange range = DateRange.of(null, LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 21));

        assertThat(range.from()).isEqualTo(Instant.parse("2026-10-19T00:00:00Z"));
        assertThat(range.to()).isEqualTo(Instant.parse("2026-10-22T00:00:00Z"));
    }

    @Test
    void noFiltersMeansUnbounded() {
        DateRange range = DateRange.of(null, null, null);

        assertThat(range.from()).isEqualTo(Instant.EPOCH);
        assertThat(range.to()).isEqualTo(Instant.parse("9999-12-31T23:59:59Z"));
    }

    @Test
    void rejectsMixingSingleDateWithRange() {
        assertThatThrownBy(() -> DateRange.of(LocalDate.now(ZoneOffset.UTC), LocalDate.now(ZoneOffset.UTC), null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsPartialRange() {
        assertThatThrownBy(() -> DateRange.of(null, LocalDate.now(ZoneOffset.UTC), null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> DateRange.of(
                null,
                LocalDate.of(2026, 10, 21),
                LocalDate.of(2026, 10, 19)
        )).isInstanceOf(BadRequestException.class);
    }
}
