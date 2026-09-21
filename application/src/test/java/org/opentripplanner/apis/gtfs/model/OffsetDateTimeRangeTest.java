package org.opentripplanner.apis.gtfs.model;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.time.TimePeriod;

class OffsetDateTimeRangeTest {

  private static final ZoneId ZONE_ID = ZoneId.of("Europe/Berlin");
  private static final Instant START = OffsetDateTime.parse(
    "2023-02-15T12:03:28+01:00"
  ).toInstant();
  private static final Instant END = OffsetDateTime.parse("2023-02-16T12:03:28+01:00").toInstant();

  @Test
  void convertBoundedPeriod() {
    var subject = OffsetDateTimeRange.of(TimePeriod.of(START, END), ZONE_ID);

    assertThat(subject.start()).isEqualTo(OffsetDateTime.parse("2023-02-15T12:03:28+01:00"));
    assertThat(subject.end()).isEqualTo(OffsetDateTime.parse("2023-02-16T12:03:28+01:00"));
  }

  @Test
  void convertUnboundedPeriod() {
    var subject = OffsetDateTimeRange.of(TimePeriod.ofUnbounded(), ZONE_ID);

    assertThat(subject.start()).isNull();
    assertThat(subject.end()).isNull();
  }

  @Test
  void sortChronologically() {
    var unboundedStart = OffsetDateTimeRange.of(TimePeriod.of(null, START), ZONE_ID);
    var bounded = OffsetDateTimeRange.of(TimePeriod.of(START, END), ZONE_ID);
    var unboundedEndFromStart = OffsetDateTimeRange.of(TimePeriod.of(START, null), ZONE_ID);
    var unboundedEnd = OffsetDateTimeRange.of(TimePeriod.of(END, null), ZONE_ID);

    var sorted = List.of(unboundedEnd, bounded, unboundedEndFromStart, unboundedStart)
      .stream()
      .sorted(OffsetDateTimeRange.CHRONOLOGICAL_ORDER)
      .toList();

    assertThat(sorted)
      .containsExactly(unboundedStart, bounded, unboundedEndFromStart, unboundedEnd)
      .inOrder();
  }
}
