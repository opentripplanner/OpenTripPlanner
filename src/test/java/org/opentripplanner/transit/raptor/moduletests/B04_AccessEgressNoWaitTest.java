package org.opentripplanner.transit.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.NO_WAIT_STD;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.STANDARD;
import static org.opentripplanner.transit.raptor.api.request.SearchDirection.REVERSE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.api.PathUtils;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;

/**
 * FEATURE UNDER TEST
 *
 * This test focus on testing access and egress with basic Raptor - NOT Range Raptor. All tests are
 * run with one iteration only. The Range Raptor make sure the best access/egress is used, but
 * without it, we should board at the first possible stop for the standard raptor, but not for the
 * no-wait-std. The reason for this is that the standard optimize on best arrival time, while the
 * no-wait-std optimize on finding the shortest travel time.
 */
public class B04_AccessEgressNoWaitTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(RaptorConfig.defaultConfigForTest());

  @BeforeEach
  public void setup() {
    data.withRoute(
        route("R1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E, STOP_F)
            .withTimetable(schedule("0:10 0:14 0:18 0:30 0:34 0:38"))
    );

    requestBuilder.searchParams()
        .addAccessPaths(
            walk(STOP_A, D1s),
            walk(STOP_B, D10s), // Best option
            walk(STOP_C, D5m)
        )
        .addEgressPaths(
            walk(STOP_D, D5m),
            walk(STOP_E, D10s), // Best option
            walk(STOP_F, D1s)
        )
        .earliestDepartureTime(T00_00)
        .latestArrivalTime(T01_00)
        .searchOneIterationOnly()
    ;
    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  /**
   * A test on the standard profile is included to demonstrate that the no-wait and the
   * standard give different results. They differ at the beginning of the path: Stop A is
   * used for standard, while stop B is found by the no-wait.
   */
  @Test
  public void standard() {
    requestBuilder.profile(STANDARD);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: Board at first possible stop A and arrive at E (the earliest arrival time)
    assertEquals(
        "Walk 1s ~ A ~ BUS R1 0:10 0:34 ~ E ~ Walk 10s [0:09:59 0:34:10 24m11s]",
        PathUtils.pathsToString(response)
    );
  }

  @Test
  public void noWaitStd() {
    requestBuilder.profile(NO_WAIT_STD);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: Board at stop B (the latest departure time) and arrive at E (the earliest arrival time)
    assertEquals(
            "Walk 10s ~ B ~ BUS R1 0:14 0:34 ~ E ~ Walk 10s [0:13:50 0:34:10 20m20s]",
            PathUtils.pathsToString(response)
    );
  }

  /**
   * A reverse test on the standard profile is included to demonstrate that the no-wait and the
   * standard give different results when searching in reverse. They differ at the end of the
   * path: Stop F is used for standard, while stop E is found by the no-wait.
   */
  @Test
  public void standardReverse() {
    requestBuilder.profile(STANDARD).searchDirection(REVERSE);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: Searching in reverse we will board at the first possible stop F and "alight" at
    // the stop giving us the latest departure time - stop B.
    assertEquals(
            "Walk 10s ~ B ~ BUS R1 0:14 0:38 ~ F ~ Walk 1s [0:13:50 0:38:01 24m11s]",
            PathUtils.pathsToString(response)
    );
  }

  @Test
  public void noWaitStdReverse() {
    requestBuilder.profile(NO_WAIT_STD).searchDirection(REVERSE);

    var response = raptorService.route(requestBuilder.build(), data);

    // Searching in reverse, the boarding happens at the end, and the alighting is in the beginning:
    // expect: Board at stop E (the latest departure time) and arrive at B (the earliest arrival time)
    assertEquals(
            "Walk 10s ~ B ~ BUS R1 0:14 0:34 ~ E ~ Walk 10s [0:13:50 0:34:10 20m20s]",
            PathUtils.pathsToString(response)
    );
  }

  @Test
  public void multiCriteria() {
    requestBuilder
            .profile(MULTI_CRITERIA);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: The multi-criteria search should find the best alternative because it looks at
    //         the arrival-time, generalized-cost, and departure-time(travel-time).
    assertEquals(
            "Walk 10s ~ B ~ BUS R1 0:14 0:34 ~ E ~ Walk 10s [0:13:50 0:34:10 20m20s $1840]",
            PathUtils.pathsToString(response)
    );
  }
}