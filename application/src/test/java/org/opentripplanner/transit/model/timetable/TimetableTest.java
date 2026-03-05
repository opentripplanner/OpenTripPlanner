package org.opentripplanner.transit.model.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;

class TimetableTest {

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();
  private final RegularStop stopA = envBuilder.stop("A");
  private final RegularStop stopC = envBuilder.stop("C");

  @ParameterizedTest
  @CsvSource(
    value = """
    Description           | Timetable      | Expected number of days
    Same day              | 08:00 22:00    | 0
    Same day, exact limit | 08:00 23:59    | 0
    Night bus             | 22:00 1:00+1d  | 1
    Overnight exact limit | 22:59 23:59+1d | 1
    2 overnights          | 1:00 1:00+2d   | 2
    """,
    delimiter = '|',
    useHeadersInDisplayName = true
  )
  void maxTripSpanDays(String testCaseName, String schedule, int expectedNumberOfDays) {
    var times = schedule.trim().split("\\s+");
    var env = envBuilder
      .addTrip(TripInput.of("t1").addStop(stopA, times[0]).addStop(stopC, times[1]))
      .build();

    var timetable = env.tripData("t1").tripPattern().getScheduledTimetable();
    assertEquals(expectedNumberOfDays, timetable.getMaxTripSpanDays());
  }

  @Test
  void getTripTimesByTrip() {
    var env = envBuilder
      .addTrip(TripInput.of("trip1").addStop(stopA, "08:00").addStop(stopC, "09:00"))
      .addTrip(TripInput.of("trip2").addStop(stopA, "10:00").addStop(stopC, "11:00"))
      .build();

    var timetable = env.tripData("trip1").tripPattern().getScheduledTimetable();
    var trip1 = env.tripData("trip1").trip();
    var trip2 = env.tripData("trip2").trip();

    assertSame(timetable.getTripTimes(trip1).getTrip(), trip1);
    assertSame(timetable.getTripTimes(trip2).getTrip(), trip2);
  }

  @Test
  void getTripTimesByFeedScopedId() {
    var env = envBuilder
      .addTrip(TripInput.of("trip1").addStop(stopA, "08:00").addStop(stopC, "09:00"))
      .addTrip(TripInput.of("trip2").addStop(stopA, "10:00").addStop(stopC, "11:00"))
      .build();

    var timetable = env.tripData("trip1").tripPattern().getScheduledTimetable();
    var trip1 = env.tripData("trip1").trip();
    var trip2 = env.tripData("trip2").trip();

    assertSame(timetable.getTripTimes(trip1.getId()).getTrip(), trip1);
    assertSame(timetable.getTripTimes(trip2.getId()).getTrip(), trip2);
  }

  @Test
  void getTripTimesUsesEqualsNotIdentity() {
    var env = envBuilder
      .addTrip(TripInput.of("sameId").addStop(stopA, "08:00").addStop(stopC, "09:00"))
      .build();

    var timetable = env.tripData("sameId").tripPattern().getScheduledTimetable();
    var trip1 = env.tripData("sameId").trip();

    // Create a different Trip instance with the same ID
    var trip2 = Trip.of(id("sameId")).withRoute(envBuilder.route("lookupRoute")).build();
    assertEquals(trip1.getId(), trip2.getId());

    // Lookup using the second instance should find the TripTimes stored with the first
    assertSame(trip1, timetable.getTripTimes(trip2).getTrip());
  }

  @Test
  void getTripTimesReturnsNullForUnknownTrip() {
    var env = envBuilder
      .addTrip(TripInput.of("trip1").addStop(stopA, "08:00").addStop(stopC, "09:00"))
      .build();

    var timetable = env.tripData("trip1").tripPattern().getScheduledTimetable();

    var unknownTrip = Trip.of(id("unknown")).withRoute(envBuilder.route("lookupRoute2")).build();
    assertNull(timetable.getTripTimes(unknownTrip));
    assertNull(timetable.getTripTimes(id("unknown")));
  }
}
