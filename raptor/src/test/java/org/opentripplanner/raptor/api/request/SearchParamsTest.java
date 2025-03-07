package org.opentripplanner.raptor.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;

class SearchParamsTest {

  @Test
  void earliestDepartureTimeOrLatestArrivalTimeIsRequired() {
    var p = new RaptorRequestBuilder<TestTripSchedule>().searchParams();
    p.addAccessPaths(TestAccessEgress.walk(1, 30));
    p.addEgressPaths(TestAccessEgress.walk(2, 20));

    assertParamNotValid(p, "'earliestDepartureTime' or 'latestArrivalTime' is required.");
  }

  @Test
  void accessPathIsRequired() {
    var p = new RaptorRequestBuilder<TestTripSchedule>().searchParams();
    p.earliestDepartureTime(200);
    p.latestArrivalTime(600);
    p.addEgressPaths(TestAccessEgress.walk(2, 20));

    assertParamNotValid(p, "At least one 'accessPath' is required.");
  }

  @Test
  void egressPathIsRequired() {
    var p = new RaptorRequestBuilder<TestTripSchedule>().searchParams();
    p.earliestDepartureTime(200);
    p.latestArrivalTime(600);
    p.addAccessPaths(TestAccessEgress.walk(1, 30));

    assertParamNotValid(p, "At least one 'egressPath' is required.");
  }

  @Test
  void latestArrivalTimeRequiredWhenDepartAsLateAsPossibleEnabled() {
    var p = new RaptorRequestBuilder<TestTripSchedule>().searchParams();
    p.earliestDepartureTime(200);
    p.addAccessPaths(TestAccessEgress.walk(1, 30));
    p.addEgressPaths(TestAccessEgress.walk(2, 20));

    p.preferLateArrival(true);

    assertParamNotValid(
      p,
      "The 'latestArrivalTime' is required when 'departAsLateAsPossible' is set."
    );
  }

  @Test
  void departAsLateAsPossibleAndTimetableEnabled() {
    var p = new RaptorRequestBuilder<TestTripSchedule>().searchParams();
    p.latestArrivalTime(200);
    p.addAccessPaths(TestAccessEgress.walk(1, 30));
    p.addEgressPaths(TestAccessEgress.walk(2, 20));

    p.timetable(true);
    p.preferLateArrival(true);

    assertParamNotValid(
      p,
      "The 'departAsLateAsPossible' is not allowed together with 'timetableEnabled'."
    );
  }

  @Test
  void viaAndPassThrough() {
    var noVia = new RaptorRequestBuilder<TestTripSchedule>().searchParams().buildSearchParam();
    var via = new RaptorRequestBuilder<TestTripSchedule>()
      .searchParams()
      .addViaLocation(RaptorViaLocation.via("Via").addViaStop(5).build())
      .buildSearchParam();
    var passThrough = new RaptorRequestBuilder<TestTripSchedule>()
      .searchParams()
      .addViaLocation(RaptorViaLocation.passThrough("Via").addPassThroughStop(5).build())
      .buildSearchParam();

    assertFalse(noVia.isVisitViaSearch());
    assertTrue(via.isVisitViaSearch());
    assertFalse(passThrough.isVisitViaSearch());

    assertFalse(noVia.isPassThroughSearch());
    assertFalse(via.isPassThroughSearch());
    assertTrue(passThrough.isPassThroughSearch());

    assertEquals("[]", toString(noVia.viaLocations()));
    assertEquals("[RaptorViaLocation{via Via : [(stop E)]}]", toString(via.viaLocations()));
    assertEquals(
      "[RaptorViaLocation{pass-through Via : [(stop E)]}]",
      toString(passThrough.viaLocations())
    );
  }

  @Test
  void addBothViaAndPassThroughIsNotSupported() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      new RaptorRequestBuilder<TestTripSchedule>()
        .searchParams()
        .earliestDepartureTime(1)
        .latestArrivalTime(1200)
        .addAccessPaths(TestAccessEgress.walk(1, 30))
        .addEgressPaths(TestAccessEgress.walk(7, 30))
        .addViaLocations(
          List.of(
            RaptorViaLocation.via("Via").addViaStop(5).build(),
            RaptorViaLocation.passThrough("PassThrough").addPassThroughStop(5).build()
          )
        )
        .build()
    );
    assertEquals(
      "Combining pass-through and regular via-vist it is not allowed: [" +
      "RaptorViaLocation{via Via : [(stop 5)]}, " +
      "RaptorViaLocation{pass-through PassThrough : [(stop 5)]}" +
      "].",
      ex.getMessage()
    );
  }

  void assertParamNotValid(SearchParamsBuilder<TestTripSchedule> p, String msg) {
    assertThrows(IllegalArgumentException.class, p::build, msg);
  }

  private static String toString(Collection<RaptorViaLocation> viaLocations) {
    return viaLocations
      .stream()
      .map(it -> it.toString(RaptorTestConstants::stopIndexToName))
      .toList()
      .toString();
  }
}
