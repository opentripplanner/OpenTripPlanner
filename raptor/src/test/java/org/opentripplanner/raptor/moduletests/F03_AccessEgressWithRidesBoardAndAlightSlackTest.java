package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.PathUtils;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorTestFactory;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;
import org.opentripplanner.raptor.spi.TestSlackProvider;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should add transit-slack + board-slack after flex access, and transit-slack + alight-slack
 * before flex egress.
 */
public class F03_AccessEgressWithRidesBoardAndAlightSlackTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  public void setup() {
    data
      //Given slacks: transfer 1m, board 30s, alight 10s
      .withSlackProvider(new TestSlackProvider(D1_m, D30_s, D10_s))
      // Start walking 1m before: 30s walk + 30s board-slack
      .access("Flex+Walk 2m Rₙ1 ~ B")
      .withRoute(
        // Pattern arrive at stop 2 at 0:03:00
        route(pattern("R1", STOP_B, STOP_C)).withTimetable(
          // First trip is too early: It takes 2m to get to the point of boarding:
          // --> 00:00:00 + flex 30s + slack(1m + 30s) = 00:02:00
          schedule().departures("0:03:29  0:05:29"),
          // This is the trip we expect to board
          schedule().departures("0:04:00 0:10:00").arrivals("0  00:06:00"),
          // REVERSE SEARCH: The last trip arrives too late: It takes 1m40s to get to the
          // point of "boarding" in the reverse search:
          // --> 00:10:00 - (flex 20s + slack(1m + 10s)) = 00:08:30  (arrival time)
          schedule().arrivals("0:04:51  0:06:51")
        )
      )
      // Ends 30s after last stop arrival: 10s alight-slack + 20s walk
      .egress("C ~ Flex 2m Rₙ1");

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_10)
      // Only one iteration is needed - the access should be time-shifted
      .searchWindowInSeconds(D3_m);

    ModuleTestDebugLogging.setupDebugLogging(data);
  }

  static List<RaptorModuleTestCase> testCases() {
    var path =
      "Flex+Walk 2m Rₙ1 ~ B ~ BUS R1 0:04 0:06 ~ C ~ Flex 2m Rₙ1 " +
      "[0:00:30 0:09:10 8m40s Tₙ2 C₁1_360]";
    return RaptorModuleTestCase.of()
      // TODO - Alight slack is missing
      //.add(TC_MIN_DURATION, "[0:00 0:08:30 8m30s 2tx]")
      // TODO - Board slack is missing
      //.add(TC_MIN_DURATION_REV, "[0:01:50 0:10 8m10s 2tx]")
      .addMinDuration("8m40s", TX_2, T00_00, T00_10)
      .add(standard(), PathUtils.withoutCost(path))
      .add(multiCriteria(), path)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
