package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.direct.api.RaptorDirectTransitRequest;

/**
 * FEATURE UNDER TEST
 * <p>
 * The direct transit search should include trips within the cost limit
 */
public class M04_DirectTransitCostLimitTest implements RaptorTestConstants {

  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  ///  Expensive trips should be included even if they are not optimal on arrival or departure
  @Test
  void testIncludeExpensive() {
    var data = new TestTransitData();
    data
      .withRoute(route(pattern("FAST", STOP_A, STOP_B)).withTimetable(schedule("01:00, 01:10")))
      .withRoute(
        route(pattern("SLOW", STOP_A, STOP_B)).withTimetable(
          schedule("00:05, 01:05"),
          schedule("01:05, 02:05")
        )
      );

    var result = raptorService.findAllDirectTransit(createRequest(), data);
    assertEquals(
      "A ~ BUS SLOW 0:05 1:05 ~ B [0:05 1:05 1h Tₙ0 C₁4_200]\n" +
        "A ~ BUS FAST 1:00 1:10 ~ B [1:00 1:10 10m Tₙ0 C₁1_200]\n" +
        "A ~ BUS SLOW 1:05 2:05 ~ B [1:05 2:05 1h Tₙ0 C₁4_200]",
      pathsToString(result)
    );
  }

  ///  Trips with a cost above the limit should be rejected when they are not optimal on arrival or departure
  @Test
  void testRejectExpensive() {
    var data = new TestTransitData();
    data
      .withRoute(route(pattern("FAST", STOP_A, STOP_B)).withTimetable(schedule("01:00, 01:10")))
      .withRoute(route(pattern("SLOWER", STOP_A, STOP_B)).withTimetable(schedule("01:00, 01:29")))
      .withRoute(route(pattern("SLOWEST", STOP_A, STOP_B)).withTimetable(schedule("01:00, 01:30")));

    var result = raptorService.findAllDirectTransit(createRequest(), data);
    assertEquals(
      "A ~ BUS FAST 1:00 1:10 ~ B [1:00 1:10 10m Tₙ0 C₁1_200]\n" +
        "A ~ BUS SLOWER 1:00 1:29 ~ B [1:00 1:29 29m Tₙ0 C₁2_340]",
      pathsToString(result)
    );
  }

  private RaptorDirectTransitRequest createRequest() {
    return RaptorDirectTransitRequest.of()
      .withRelaxC1(GeneralizedCostRelaxFunction.of(2))
      .earliestDepartureTime(T00_00)
      .searchWindowInSeconds(D24_h)
      .addAccessPaths(TestAccessEgress.free(STOP_A))
      .addEgressPaths(TestAccessEgress.free(STOP_B))
      .build();
  }
}
