package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION_REV;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.multiCriteria;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.standard;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.PathUtils;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;

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
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  void setup() {
    //Given slack: transfer 1m, board 30s, alight 10s
    data.withSlackProvider(new DefaultSlackProvider(D1m, D30s, D10s));

    data.withRoute(
      // Pattern arrive at stop 2 at 0:03:00
      route(pattern("R1", STOP_B, STOP_C))
        .withTimetable(schedule().departures("00:02:11 00:03:11").arrDepOffset(D10s))
    );
    data.withRoute(
      // earliest-departure-time: 0:03:00 + 10s + 1m + 30s = 0:04:40
      route(pattern("R2", STOP_C, STOP_D))
        .withTimetable(
          schedule().departures("00:04:40 00:05:10").arrDepOffset(D10s), // Missed by 1 second
          schedule().departures("00:04:41 00:05:11").arrDepOffset(D10s) // Exact match
        )
    );
    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.walk(STOP_B, D30s)) // Start walking 1m before: 30s walk + 30s board-slack
      .addEgressPaths(TestAccessEgress.walk(STOP_D, D20s)) // Ends 30s after last stop arrival: 10s alight-slack + 20s walk
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .searchWindowInSeconds(D3m);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    // TODO expMinDuration= walk 50s + Transit 1m20s + 1x transfer 1m + 2x board/alight slack 1m20s
    //      = 4m30s

    var expected =
      "Walk 30s ~ B " +
      "~ BUS R1 0:02:11 0:03:01 ~ C " +
      "~ BUS R2 0:04:41 0:05:01 ~ D " +
      "~ Walk 20s " +
      "[0:01:11 0:05:31 4m20s 1tx $1510]";

    return RaptorModuleTestCase
      .of()
      // TODO: 2x10s alight slack is ignored in forward search for min-duration
      .add(TC_MIN_DURATION, "[0:00 0:04 4m 1tx]")
      // TODO: 2x30s Board- and 1x1m transfer slack is ignored in reverse search for min-duration
      .add(TC_MIN_DURATION_REV, "[0:26:40 0:30 3m20s 1tx]")
      .add(standard(), PathUtils.withoutCost(expected))
      .add(multiCriteria(), expected)
      .build();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testRaptor(RaptorModuleTestCase testCase) {
    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }
}
