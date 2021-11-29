package org.opentripplanner.transit.raptor.moduletests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripPattern;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.request.SearchDirection;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should return a path if it exists for a basic case with one route with one trip including boarding/alighting
 * restrictions and, an access and an egress path.
 */
public class A02_SingeRouteRestrictionsTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(RaptorConfig.defaultConfigForTest());

  /**
   * Stops: 0..3
   *
   * Route restrictions at stop (B:Board, A:Alight, W:Wheelchair):
   *   Stop:  B   C   D
   *     R1:  BW  BA  AW
   *
   * Schedule:
   *   Stop:     B      C      D
   *     R1:   00:01  00:03  00:05
   *
   * Access (toStop & duration):
   *   1  30s
   *
   * Egress (fromStop & duration):
   *   3  20s
   *
   */
  @BeforeEach
  public void setup() {
    TestTripPattern pattern = pattern("R1", STOP_B, STOP_C, STOP_D);
    pattern.restrictions("BW BA AW");
    data.withRoute(
        route(
            pattern
        )
        .withTimetable(
            schedule("00:01, 00:03, 00:05")
        )
    );
    requestBuilder.searchParams()
        .addAccessPaths(walk(STOP_B, D30s))
        .addEgressPaths(walk(STOP_D, D20s))
        .earliestDepartureTime(T00_00)
        .latestArrivalTime(T00_10)
        .timetableEnabled(true);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void standard() {
    var request = requestBuilder
        .profile(RaptorProfile.STANDARD)
        .build();

    var response = raptorService.route(request, data);

    assertEquals(
        "Walk 30s ~ B ~ BUS R1 0:01 0:05 ~ D ~ Walk 20s [0:00:30 0:05:20 4m50s]",
        pathsToString(response)
    );
  }

  @Test
  public void standardReverse() {
    var request = requestBuilder
        .searchDirection(SearchDirection.REVERSE)
        .profile(RaptorProfile.STANDARD)
        .build();

    var response = raptorService.route(request, data);

    assertEquals(
        "Walk 30s ~ B ~ BUS R1 0:01 0:05 ~ D ~ Walk 20s [0:00:30 0:05:20 4m50s]",
        pathsToString(response)
    );
  }

  @Test
  public void multiCriteria() {
    var request = requestBuilder
        .profile(RaptorProfile.MULTI_CRITERIA)
        .build();

    var response = raptorService.route(request, data);

    assertEquals(
        "Walk 30s ~ B ~ BUS R1 0:01 0:05 ~ D ~ Walk 20s [0:00:30 0:05:20 4m50s $940]",
        pathsToString(response)
    );
  }
}