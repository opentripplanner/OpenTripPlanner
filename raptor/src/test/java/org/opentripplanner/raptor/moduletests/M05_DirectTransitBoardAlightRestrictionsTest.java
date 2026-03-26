package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripPattern;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.extentions.direct.api.RaptorDirectTransitRequest;

/**
 * FEATURE UNDER TEST
 * <p>
 * The direct transit search should respect boarding and alighting restrictions
 */
public class M05_DirectTransitBoardAlightRestrictionsTest implements RaptorTestConstants {

  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @Test
  void testExcludeByBoardingAlighting() {
    var data = new TestTransitData().withRoute(
      route(
        TestTripPattern.of("P1", STOP_A, STOP_B, STOP_C, STOP_D).restrictions("B A B A").build()
      ).withTimetable(schedule("01:00, 02:00 03:00, 04:00"))
    );

    var aToD = raptorService.findAllDirectTransit(createRequest(STOP_A, STOP_D), data);
    assertEquals("A ~ BUS P1 1:00 4:00 ~ D [1:00 4:00 3h Tₙ0 C₁11_400]", pathsToString(aToD));

    var bToC = raptorService.findAllDirectTransit(createRequest(STOP_B, STOP_D), data);
    assertTrue(bToC.isEmpty());

    var aToC = raptorService.findAllDirectTransit(createRequest(STOP_A, STOP_C), data);
    assertTrue(aToC.isEmpty());
  }

  private RaptorDirectTransitRequest createRequest(int fromStop, int toStop) {
    return RaptorDirectTransitRequest.of()
      .withRelaxC1(GeneralizedCostRelaxFunction.of(2))
      .earliestDepartureTime(T00_00)
      .searchWindowInSeconds(D24_h)
      .addAccessPaths(TestAccessEgress.free(fromStop))
      .addEgressPaths(TestAccessEgress.free(toStop))
      .build();
  }
}
