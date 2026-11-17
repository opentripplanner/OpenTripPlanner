package org.opentripplanner.transit.model.calendar;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.routing.graph.kryosupport.KryoTestHelper.writeAndRead;

import com.carrotsearch.hppc.IntHashSet;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The service codes running for a date are HPPC {@link IntHashSet}s, which have no dedicated Kryo
 * serializer. This verifies they survive the graph serialization round trip.
 */
class TripCalendarsKryoTest {

  private static final LocalDate DATE = LocalDate.of(2024, 1, 10);

  @Test
  void serviceCodesRunningForDateSurviveRoundTrip() {
    var calendars = calendars();

    var read = writeAndRead(calendars);

    var expected = calendars.getServiceCodesRunningForDate();
    var actual = read.getServiceCodesRunningForDate();
    assertEquals(expected.keySet(), actual.keySet());
    assertEquals(expected, actual);
    assertThat(actual.get(DATE).toArray()).asList().containsExactly(0, 1);
    assertTrue(actual.get(DATE).contains(0));
    assertTrue(actual.get(DATE).contains(1));
  }

  private static TripCalendars calendars() {
    var data = TripCalendars.of();
    data.putServiceDatesForServiceId(id("A"), List.of(DATE, DATE.plusDays(1)));
    data.putServiceDatesForServiceId(id("B"), List.of(DATE));
    return TripCalendars.empty()
      .merge(data.build())
      .withNextServiceCodes(List.of(id("A"), id("B")))
      .initializeServiceCodesRunningForDate();
  }
}
