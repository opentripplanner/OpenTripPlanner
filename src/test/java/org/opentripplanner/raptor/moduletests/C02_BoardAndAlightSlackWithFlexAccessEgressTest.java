package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flexAndWalk;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.spi.RaptorSlackProvider.defaultSlackProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.spi.SearchDirection;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should add transit-slack + board-slack after flex access, and transit-slack + alight-slack
 * before flex egress.
 */
public class C02_BoardAndAlightSlackWithFlexAccessEgressTest implements RaptorTestConstants {

  /** The expected result is tha same for all tests */
  private static final String EXPECTED_RESULT =
    "Flex 2m 1x ~ B ~ " + "BUS R1 0:04 0:06 ~ C ~ " + "Flex 2m 1x " + "[0:00:30 0:09:10 8m40s 2tx]";
  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    //Given slack: transfer 1m, board 30s, alight 10s
    requestBuilder.slackProvider(defaultSlackProvider(D1m, D30s, D10s));

    data.withRoute(
      // Pattern arrive at stop 2 at 0:03:00
      route(pattern("R1", STOP_B, STOP_C))
        .withTimetable(
          // First trip is too early: It takes 2m to get to the point of boarding:
          // --> 00:00:00 + flex 30s + slack(1m + 30s) = 00:02:00
          schedule().departures("0:03:29, 0:05:29"),
          // This is the trip we expect to board
          schedule().departures("0:04:00, 0:10:00").arrivals("0, 00:06:00"),
          // REVERSE SEARCH: The last trip arrives to late: It takes 1m40s to get to the
          // point of "boarding" in the reverse search:
          // --> 00:10:00 - (flex 20s + slack(1m + 10s)) = 00:08:30  (arrival time)
          schedule().arrivals("0:04:51, 0:06:51")
        )
    );
    requestBuilder
      .searchParams()
      // Start walking 1m before: 30s walk + 30s board-slack
      .addAccessPaths(flexAndWalk(STOP_B, D2m, ONE_RIDE, 40_000))
      // Ends 30s after last stop arrival: 10s alight-slack + 20s walk
      .addEgressPaths(flex(STOP_C, D2m, ONE_RIDE, 56_000))
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_10)
      // Only one iteration is needed - the access should be time-shifted
      .searchWindowInSeconds(D3m);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void standard() {
    var request = requestBuilder
      .profile(RaptorProfile.STANDARD)
      .searchParams()
      .searchOneIterationOnly()
      .build();

    var response = raptorService.route(request, data);
    assertEquals(EXPECTED_RESULT, pathsToString(response));
  }

  @Test
  public void standardReverse() {
    var request = requestBuilder
      .profile(RaptorProfile.STANDARD)
      .searchDirection(SearchDirection.REVERSE)
      .searchParams()
      .searchOneIterationOnly()
      .build();
    var response = raptorService.route(request, data);
    assertEquals(EXPECTED_RESULT, pathsToString(response));
  }

  @Test
  public void multiCriteria() {
    // Add cost to result string
    String expected = EXPECTED_RESULT.replace("]", " $1840]");
    var request = requestBuilder.profile(RaptorProfile.MULTI_CRITERIA).build();
    var response = raptorService.route(request, data);
    assertEquals(expected, pathsToString(response));
  }
}
