package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flexAndWalk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTransfer.transfer;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MULTI_CRITERIA;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.moduletests.support.RaptorModuleTestCase;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should be able to timeshift a Flex ~ Walk ~ Flex path.
 */
public class F04_AccessEgressWithRidesNoTransitTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data
      .withRoute(
        // Pattern arrive at stop 2 at 0:03:00
        route(pattern("R1", STOP_A, STOP_D))
          .withTimetable(
            // First trip is too early: It takes 2m to get to the point of boarding:
            // --> 00:00:00 + flex 30s + slack(1m + 30s) = 00:02:00
            schedule().departures("0:03:29, 0:05:29"),
            // This is the trip we expect to board
            schedule().departures("0:04:00, 0:10:00").arrivals("0, 00:06:00"),
            // REVERSE SEARCH: The last trip arrives too late: It takes 1m40s to get to the
            // point of "boarding" in the reverse search:
            // --> 00:10:00 - (flex 20s + slack(1m + 10s)) = 00:08:30  (arrival time)
            schedule().arrivals("0:04:51, 0:06:51")
          )
      )
      .withTransfer(RaptorTestConstants.STOP_B, transfer(STOP_C, D5m));
    requestBuilder
      .searchParams()
      .addEgressPaths(flex(STOP_C, D2m, ONE_RIDE, 56_000))
      .earliestDepartureTime(T00_10)
      .latestArrivalTime(T00_30)
      // Only one iteration is needed - the access should be time-shifted
      .searchWindowInSeconds(D10m);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  void withOpeningHours() {
    requestBuilder
      .searchParams()
      .addAccessPaths(flexAndWalk(STOP_B, D2m, ONE_RIDE, 40_000).openingHours("00:05", "00:10"));
    var path =
      "Flex+Walk 2m 1x Open(0:05 0:10) ~ B ~ Walk 5m ~ C ~ Flex 2m 1x [0:15 0:25 10m 1tx $1620]";
    var testCase = RaptorModuleTestCase.of().add(TC_MULTI_CRITERIA, path).build().get(0);
    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }

  @Test
  void noOpeningHours() {
    requestBuilder.searchParams().addAccessPaths(flexAndWalk(STOP_B, D2m, ONE_RIDE, 40_000));
    var path = "Flex+Walk 2m 1x ~ B ~ Walk 5m ~ C ~ Flex 2m 1x [0:15 0:25 10m 1tx $1620]";
    var testCase = RaptorModuleTestCase.of().add(TC_MULTI_CRITERIA, path).build().get(0);
    var request = testCase.withConfig(requestBuilder);
    var response = raptorService.route(request, data);
    assertEquals(testCase.expected(), pathsToString(response));
  }
}
