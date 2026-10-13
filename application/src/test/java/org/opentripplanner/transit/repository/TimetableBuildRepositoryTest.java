package org.opentripplanner.transit.repository;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

class TimetableBuildRepositoryTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final TripPattern PATTERN = TransitRepositoryForTest.tripPattern(
    "P1",
    TransitRepositoryForTest.route("R1").build()
  )
    .withStopPattern(
      TransitRepositoryForTest.stopPattern(
        TEST_MODEL.stop("S1").build(),
        TEST_MODEL.stop("S2").build()
      )
    )
    .build();

  @Test
  void modificationsFailAfterConversion() {
    var subject = new TimetableBuildRepository();
    subject.toScheduledTimetableData();

    assertThrows(IllegalStateException.class, () ->
      subject.addTripPattern(PATTERN.getId(), PATTERN)
    );
    assertThrows(IllegalStateException.class, () ->
      subject.addTripOnServiceDate(
        TripOnServiceDate.of(id("TOSD1"))
          .withTrip(TransitRepositoryForTest.trip("T1").build())
          .withServiceDate(LocalDate.of(2025, 2, 28))
          .build()
      )
    );
    assertThrows(IllegalStateException.class, () -> subject.addFlexTrip(id("F1"), null));
    assertThrows(IllegalStateException.class, () ->
      subject.updateCalendarServiceData(new CalendarServiceData())
    );
    assertThrows(IllegalStateException.class, () -> subject.putServiceCode(id("S1"), 0));
  }

  @Test
  void readingAndConvertingAgainWorkAfterConversion() {
    var subject = new TimetableBuildRepository();
    subject.addTripPattern(PATTERN.getId(), PATTERN);

    assertThat(subject.toScheduledTimetableData().getAllTripPatterns()).containsExactly(PATTERN);
    assertThat(subject.getAllTripPatterns()).containsExactly(PATTERN);
    assertThat(subject.toScheduledTimetableData().getAllTripPatterns()).containsExactly(PATTERN);
  }
}
