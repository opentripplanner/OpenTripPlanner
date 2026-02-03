package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.direct.api.RaptorDirectTransitRequest;

/**
 * FEATURE UNDER TEST
 * <p>
 * The direct transit search should return both the optimal path and the slightly slower
 * path - the `relaxC1` define the slack.  Non-optimal paths should not be returned.
 */
class M01_DirectTransitWithRoutesWithinRelaxC1 implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  void setup() {
    data
      .withRoute(route(pattern("R1", STOP_B, STOP_D)).withTimetable(schedule("00:02 00:03:40")))
      .withRoute(route(pattern("R2", STOP_B, STOP_D)).withTimetable(schedule("00:02 00:08:59")))
      .withRoute(route(pattern("R3", STOP_B, STOP_D)).withTimetable(schedule("00:02 00:09:00")))
      .withBoardCost(0);
  }

  @Test
  void testRelaxedLimitedTransferSearch() {
    var request = RaptorDirectTransitRequest.of()
      .earliestDepartureTime(T00_00)
      .searchWindowInSeconds(D10m)
      .addAccessPaths(TestAccessEgress.walk(STOP_B, D30s))
      .addEgressPaths(TestAccessEgress.walk(STOP_D, D20s))
      .withRelaxC1(GeneralizedCostRelaxFunction.of(2.0, RaptorCostConverter.toRaptorCost(D2m)))
      .build();

    var paths = raptorService.findAllDirectTransit(request, data);

    // The best option has a generalized-cost of C₁200 (access:60 + transit:100 + egress:40).
    // With a `relaxC1(c1) -> 2.0 * c1 + 120` the c1 limit is <C₁520.
    //
    assertEquals(
      """
      Walk 30s ~ B ~ BUS R1 0:02 0:03:40 ~ D ~ Walk 20s [0:01:30 0:04 2m30s Tₙ0 C₁200]
      Walk 30s ~ B ~ BUS R2 0:02 0:08:59 ~ D ~ Walk 20s [0:01:30 0:09:19 7m49s Tₙ0 C₁519]""",
      pathsToString(paths)
    );
  }
}
