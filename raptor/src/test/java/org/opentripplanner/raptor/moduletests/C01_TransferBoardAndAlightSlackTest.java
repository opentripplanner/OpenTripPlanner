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
 * Raptor should find the correct path using the given the transfer-slack, board-slack,
 * alight-slack.
 * <p>
 * The expected result is an itinerary with 3 transit legs:
 * <p>
 * <pre>
 *    Walk 1m ~ R1 ~ C ~ R2 ~ Walk ~ R3 ~ Walk 1m
 * </pre>
 */
public class C01_TransferBoardAndAlightSlackTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = data.requestBuilder();
  private final RaptorService<TestTripSchedule> raptorService = RaptorTestFactory.raptorService();

  @BeforeEach
  void setup() {
    //Given slack: transfer 1m, board 30s, alight 10s
    data.withSlackProvider(new TestSlackProvider(D1_m, D30_s, D10_s));

    data
      // Start walking 1m before: 30s walk + 30s board-slack
      .access("Walk 30s ~ B")
      .withRoute(
        // Pattern arrive at stop 2 at 0:03:00
        route(pattern("R1", STOP_B, STOP_C)).withTimetable(
          schedule().departures("00:02:11 00:03:11").arrDepOffset(D10_s)
        )
      )
      .withRoute(
        // earliest-departure-time: 0:03:00 + 10s + 1m + 30s = 0:04:40
        route(pattern("R2", STOP_C, STOP_D)).withTimetable(
          // Missed by 1 second
          schedule().departures("00:04:40 00:05:10").arrDepOffset(D10_s),
          // Exact match
          schedule().departures("00:04:41 00:05:11").arrDepOffset(D10_s)
        )
      )
      // Ends 30s after last stop arrival: 10s alight-slack + 20s walk
      .egress("D ~ Walk 20s");

    requestBuilder
      .searchParams()
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .searchWindowInSeconds(D3_m);

    ModuleTestDebugLogging.setupDebugLogging(data);
  }

  static List<RaptorModuleTestCase> testCases() {
    var expected =
      "Walk 30s ~ B " +
      "~ BUS R1 0:02:11 0:03:01 ~ C " +
      "~ BUS R2 0:04:41 0:05:01 ~ D " +
      "~ Walk 20s " +
      "[0:01:11 0:05:31 4m20s Tₙ1 C₁1_510]";

    return RaptorModuleTestCase.of()
      .addMinDuration("4m20s", TX_1, T00_00, T00_30)
      .add(standard(), PathUtils.withoutCost(expected))
      .add(multiCriteria(), expected)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    assertEquals(testCase.expected(), testCase.run(raptorService, data, requestBuilder));
  }
}
