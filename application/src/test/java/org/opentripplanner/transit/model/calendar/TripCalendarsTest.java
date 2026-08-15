package org.opentripplanner.transit.model.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.model.calendar.CalendarServiceData;

class TripCalendarsTest {

  private static final LocalDate START = LocalDate.of(2024, 1, 10);
  private static final LocalDate END = LocalDate.of(2024, 1, 20);

  @Test
  void getOrCreateServiceIdForDateOnEmptyCalendarReturnsNull() {
    TripCalendars empty = TripCalendars.empty();

    var result = empty.getOrCreateServiceIdForDate(LocalDate.of(2024, 1, 1), updated -> {
      throw new AssertionError("onUpdate must not be called for an empty calendar");
    });

    assertNull(result, "an empty calendar has no service period, so no date is 'within' it");
  }

  @Test
  void getOrCreateServiceIdForDateOutsideServicePeriodReturnsNull() {
    TripCalendars calendar = calendarWithRange();

    assertNull(calendar.getOrCreateServiceIdForDate(START.minusDays(1), throwOnUpdate()));
    assertNull(calendar.getOrCreateServiceIdForDate(END.plusDays(1), throwOnUpdate()));
  }

  @Test
  void getOrCreateServiceIdForDateOnBoundaryDatesSucceeds() {
    TripCalendars calendar = calendarWithRange();

    assertNotNull(calendar.getOrCreateServiceIdForDate(START, updated -> {}));
    assertNotNull(calendar.getOrCreateServiceIdForDate(END, updated -> {}));
  }

  @Test
  void getOrCreateServiceIdForDateRegistersOnceThenReadsThroughOnSubsequentCalls() {
    TripCalendars calendar = calendarWithRange();
    LocalDate newDate = START.plusDays(2);

    var updateCount = new int[1];
    var updatedHolder = new TripCalendars[1];
    var serviceId = calendar.getOrCreateServiceIdForDate(newDate, updated -> {
      updateCount[0]++;
      updatedHolder[0] = updated;
      assertFalse(updated.getServiceCodesRunningForDate().get(newDate).isEmpty());
    });

    assertEquals(1, updateCount[0], "onUpdate must fire exactly once for a genuinely new date");
    TripCalendars updated = updatedHolder[0];

    // Calling again on the UPDATED instance for the same date must not re-fire onUpdate, since
    // the service id is already registered on it — the common, no-op case.
    var sameServiceId = updated.getOrCreateServiceIdForDate(newDate, ignored -> {
      throw new AssertionError("onUpdate must not fire again for an already-registered date");
    });
    assertEquals(serviceId, sameServiceId);
  }

  private static TripCalendars calendarWithRange() {
    CalendarServiceData data = new CalendarServiceData();
    data.putServiceDatesForServiceId(
      FeedScopedIdForTestFactory.id("CAL_1"),
      List.of(START, START.plusDays(1), END)
    );
    return TripCalendars.empty().merge(data);
  }

  private static java.util.function.Consumer<TripCalendars> throwOnUpdate() {
    return updated -> {
      throw new AssertionError("onUpdate must not be called for a date outside the service period");
    };
  }
}
