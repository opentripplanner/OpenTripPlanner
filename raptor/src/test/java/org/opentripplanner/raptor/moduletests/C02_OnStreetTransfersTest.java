package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.walk;
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
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.ModuleTestDebugLogging;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should use reversed transfers when searching in reverse, so that directional transfers are
 * used correctly.
 * <p>
 * The expected result is an itinerary with 2 transit legs and a transfer.
 */
public class C02_OnStreetTransfersTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    //Given slack: transfer 30s, board 0s, alight 0s
    data.withSlackProvider(new DefaultSlackProvider(D30s, D0s, D0s));

    data.withRoute(
      route(pattern("R1", STOP_B, STOP_C)).withTimetable(
        schedule().departures("00:02:00, 00:03:10").arrDepOffset(D10s)
      )
    );

    // It is not possible to transfer from D -> C
    data.withTransfer(STOP_C, TestTransfer.transfer(STOP_D, D30s));

    data.withRoute(
      route(pattern("R2", STOP_D, STOP_E)).withTimetable(
        schedule().departures("00:03:59, 00:05:09").arrDepOffset(D10s), // Missed by 1 second
        schedule().departures("00:04:00, 00:05:10").arrDepOffset(D10s) // Exact match
      )
    );

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_B, D30s)) // Start walking 1m before: 30s walk + 30s board-slack
      .addEgressPaths(walk(STOP_E, D20s)) // Ends 30s after last stop arrival: 10s alight-slack + 20s walk
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      .searchWindowInSeconds(D3m);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  static List<RaptorModuleTestCase> testCases() {
    var expected =
      "Walk 30s ~ B ~ " +
      "BUS R1 0:02 0:03 ~ C ~ " +
      "Walk 30s ~ D ~ " +
      "BUS R2 0:04 0:05 ~ E ~ " +
      "Walk 20s " +
      "[0:01:30 0:05:20 3m50s Tₓ1 C₁1_510]";
    return RaptorModuleTestCase.of()
      .addMinDuration("3m50s", TX_1, T00_00, T00_30)
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
