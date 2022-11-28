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
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.spi.SearchDirection;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should use reversed transfers when searching in reverse, so that directional transfers are
 * used correctly.
 * <p>
 * The expected result is an itinerary with 2 transit legs and a transfer.
 */
public class C03_OnStreetTransfersTest implements RaptorTestConstants {

  /** The expected result is tha same for all tests */
  private static final String EXPECTED_RESULT =
    "Walk 30s ~ B ~ " +
    "BUS R1 0:02 0:03 ~ C ~ " +
    "Walk 30s ~ D ~ " +
    "BUS R2 0:04 0:05 ~ E ~ " +
    "Walk 20s " +
    "[0:01:30 0:05:20 3m50s 1tx]";
  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    //Given slack: transfer 1m, board 30s, alight 10s
    requestBuilder.slackProvider(defaultSlackProvider(D30s, 0, 0));

    data.withRoute(
      route(pattern("R1", STOP_B, STOP_C))
        .withTimetable(schedule().departures("00:02:00, 00:03:10").arrDepOffset(D10s))
    );

    // It is not possible to transfer from D -> C
    data.withTransfer(STOP_C, TestTransfer.transfer(STOP_D, D30s));

    data.withRoute(
      route(pattern("R2", STOP_D, STOP_E))
        .withTimetable(
          schedule().departures("00:03:59, 00:05:09").arrDepOffset(D10s), // Missed by 1 second
          schedule().departures("00:04:00, 00:05:10").arrDepOffset(D10s) // Exact match
        )
    );

    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.walk(STOP_B, D30s)) // Start walking 1m before: 30s walk + 30s board-slack
      .addEgressPaths(TestAccessEgress.walk(STOP_E, D20s)) // Ends 30s after last stop arrival: 10s alight-slack + 20s walk
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
