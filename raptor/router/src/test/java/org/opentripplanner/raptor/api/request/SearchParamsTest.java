package org.opentripplanner.raptor.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;

import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.via.RaptorViaLocation;

class SearchParamsTest {

  private static final int EARLIEST_DEPARTURE_TIME = 200;
  private static final int LATEST_ARRIVAL_TIME = 1200;

  @Test
  void earliestDepartureTimeOrLatestArrivalTimeIsRequired() {
    var p = new RaptorRequestBuilder<TestTripSchedule>()
      .searchParams()
      .addAccessPaths(walk(1, 30))
      .addEgressPaths(walk(2, 20));

    assertParamNotValid(p, "'earliestDepartureTime' or 'latestArrivalTime' is required.");
  }

  @Test
  void accessPathIsRequired() {
    var p = searchParamBuilder();
    p.addEgressPaths(walk(2, 20));

    assertParamNotValid(p, "At least one 'accessPath' is required.");
  }

  @Test
  void egressPathIsRequired() {
    var p = searchParamBuilder().addAccessPaths(walk(1, 30));

    assertParamNotValid(p, "At least one 'egressPath' is required.");
  }

  @Test
  void latestArrivalTimeRequiredWhenDepartAsLateAsPossibleEnabled() {
    var p = new RaptorRequestBuilder<TestTripSchedule>()
      .searchParams()
      .earliestDepartureTime(200)
      .addAccessPaths(walk(1, 30))
      .addEgressPaths(walk(2, 20));

    p.preferLateArrival(true);

    assertParamNotValid(
      p,
      "The 'latestArrivalTime' is required when 'departAsLateAsPossible' is set."
    );
  }

  @Test
  void departAsLateAsPossibleAndTimetableEnabled() {
    var p = searchParamBuilder()
      .addAccessPaths(walk(1, 30))
      .addEgressPaths(walk(2, 20))
      .timetable(true)
      .preferLateArrival(true);

    assertParamNotValid(
      p,
      "The 'departAsLateAsPossible' is not allowed together with 'timetableEnabled'."
    );
  }

  @Test
  void viaAndPassThrough() {
    var noVia = searchParamBuilder().buildSearchParam();
    var via = searchParamBuilder()
      .addViaLocation(RaptorViaLocation.viaVisit("Via").addStop(5).build())
      .buildSearchParam();
    var passThrough = searchParamBuilder()
      .addViaLocation(RaptorViaLocation.passThrough("Via").addStop(5).build())
      .buildSearchParam();

    assertEquals("[]", toString(noVia.viaLocations()));
    assertEquals("[RaptorViaLocation{via-visit Via : [(stop E)]}]", toString(via.viaLocations()));
    assertEquals(
      "[RaptorViaLocation{pass-through Via : [(stop E)]}]",
      toString(passThrough.viaLocations())
    );
  }

  @Test
  void viaVisitAccessWithNegativeViaVisitsIsRejected() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      searchParamBuilder()
        .addAccessPaths(walk(1, 30).withViaLocationsVisited(-1))
        .addEgressPaths(walk(7, 30))
        .addViaLocation(RaptorViaLocation.viaVisit("Via").addStop(5).build())
        .build()
    );
    assertEquals("Access cannot have negative via visits: -1", ex.getMessage());
  }

  @Test
  void viaVisitAccessExceedingTotalViaLocationsIsRejected() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      searchParamBuilder()
        .addAccessPaths(walk(1, 30).withViaLocationsVisited(2))
        .addEgressPaths(walk(7, 30))
        .addViaLocation(RaptorViaLocation.viaVisit("Via").addStop(5).build())
        .build()
    );
    assertEquals("Access visits 2 via locations, but only 1 are defined", ex.getMessage());
  }

  @Test
  void viaVisitEgressExceedingTotalViaLocationsIsRejected() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      searchParamBuilder()
        .addAccessPaths(walk(1, 30))
        .addEgressPaths(walk(7, 30).withViaLocationsVisited(2))
        .addViaLocation(RaptorViaLocation.viaVisit("Via").addStop(5).build())
        .build()
    );
    assertEquals("Egress visits 2 via locations, but only 1 are defined", ex.getMessage());
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

  private static SearchParamsBuilder searchParamBuilder() {
    return new RaptorRequestBuilder<TestTripSchedule>()
      .searchParams()
      .earliestDepartureTime(EARLIEST_DEPARTURE_TIME)
      .latestArrivalTime(LATEST_ARRIVAL_TIME);
  }
}
