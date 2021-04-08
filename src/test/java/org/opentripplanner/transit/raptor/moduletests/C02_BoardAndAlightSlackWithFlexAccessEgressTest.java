package org.opentripplanner.transit.raptor.moduletests;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.request.SearchDirection;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.flex;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.flexAndWalk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider.defaultSlackProvider;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should add transit-slack + board-slack after flex access, and transit-slack + alight-slack
 * before flex egress.
 */
public class C02_BoardAndAlightSlackWithFlexAccessEgressTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
      RaptorConfig.defaultConfigForTest()
  );

  /** The expected result is tha same for all tests */
  private static final String EXPECTED_RESULT
      = "Flex 2m 1tx ~ 2 ~ "
      + "BUS R1 0:04 0:06 ~ 3 ~ "
      + "Flex 2m 1tx "
      + "[00:00:30 00:09:10 8m40s]";

  @Before
  public void setup() {
    //Given slack: transfer 1m, board 30s, alight 10s
    requestBuilder.slackProvider(
        defaultSlackProvider(D1m, D30s, D10s)
    );

    data.add(
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
    requestBuilder.searchParams()
        // Start walking 1m before: 30s walk + 30s board-slack
        .addAccessPaths(flexAndWalk(STOP_B, D2m))
        // Ends 30s after last stop arrival: 10s alight-slack + 20s walk
        .addEgressPaths(flex(STOP_C, D2m))
        .earliestDepartureTime(T00_00)
        .latestArrivalTime(T00_10)
        // Only one iteration is needed - the access should be time-shifted
        .searchWindowInSeconds(D3m)
    ;

    // Enable Raptor debugging by configuring the requestBuilder
    // data.debugToStdErr(requestBuilder);
  }

  @Test
  public void standard() {
    var request = requestBuilder.profile(RaptorProfile.STANDARD).build();
    var response = raptorService.route(request, data);
    assertEquals(EXPECTED_RESULT, pathsToString(response));
  }

  @Test
  public void standardReverse() {
    var request = requestBuilder
        .searchDirection(SearchDirection.REVERSE)
        .profile(RaptorProfile.STANDARD)
        .build();
    var response = raptorService.route(request, data);
    assertEquals(EXPECTED_RESULT, pathsToString(response));
  }

  @Test
  public void multiCriteria() {
    // Add cost to result string
    String expected = EXPECTED_RESULT.replace("]", ", cost: 1840]");
    var request = requestBuilder.profile(RaptorProfile.MULTI_CRITERIA).build();
    var response = raptorService.route(request, data);
    assertEquals(expected, pathsToString(response));
  }
}