package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.spi.RaptorSlackProvider.defaultSlackProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.spi.SearchDirection;

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

  /** The expected result is tha same for all tests */
  private static final String EXPECTED_RESULT =
    "Walk 30s ~ B ~ " +
    "BUS R1 0:02:11 0:03:01 ~ C ~ " +
    "BUS R2 0:04:41 0:05:01 ~ D ~ " +
    "Walk 20s " +
    "[0:01:11 0:05:31 4m20s 1tx]";
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
        .withTimetable(schedule().departures("00:02:11, 00:03:11").arrDepOffset(D10s))
    );
    data.withRoute(
      // earliest-departure-time: 0:03:00 + 10s + 1m + 30s = 0:04:40
      route(pattern("R2", STOP_C, STOP_D))
        .withTimetable(
          schedule().departures("00:04:40, 00:05:10").arrDepOffset(D10s), // Missed by 1 second
          schedule().departures("00:04:41, 00:05:11").arrDepOffset(D10s) // Exact match
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
    String expected = EXPECTED_RESULT.replace("]", " $1510]");
    var request = requestBuilder.profile(RaptorProfile.MULTI_CRITERIA).build();
    var response = raptorService.route(request, data);
    assertEquals(expected, pathsToString(response));
  }
}
