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
public class A04_NoWaitTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(RaptorConfig.defaultConfigForTest());

  private final String OPTIMAL_PATH = "Walk 1m ~ A ~ BUS R1_2 0:14 0:18 ~ C ~ BUS R2 0:21 0:31 ~ F ~ BUS R3_2 0:35 0:40 ~ H ~ Walk 1m [0:13 0:41 28m";

  private final String EXP_PATH_NO_WAIT = OPTIMAL_PATH + "]";
  private final String EXP_PATH_MC = OPTIMAL_PATH + " $3600]";

  @BeforeEach
  public void setup() {
    // The optimal path with respect to travel time is: A ~ R1_2 ~ C ~ R2 ~ F ~ R3_2 ~ H
    data.withRoute(route("R1_1", STOP_A, STOP_B).withTimetable(schedule("0:10 0:18")));
    data.withRoute(route("R1_2", STOP_A, STOP_C).withTimetable(schedule("0:14 0:18")));
    data.withRoute(route("R1_3", STOP_A, STOP_D).withTimetable(schedule("0:12 0:18")));
    data.withRoute(route("R2", STOP_B, STOP_C, STOP_D, STOP_E, STOP_F, STOP_G)
            .withTimetable(schedule("0:20 0:21 0:22 0:30 0:31 0:32")));
    data.withRoute(route("R3_1", STOP_E, STOP_H).withTimetable(schedule("0:35 0:42")));
    data.withRoute(route("R3_2", STOP_F, STOP_H).withTimetable(schedule("0:35 0:40")));
    data.withRoute(route("R3_3", STOP_G, STOP_H).withTimetable(schedule("0:35 0:44")));

    requestBuilder.searchParams()
            .addAccessPaths(walk(STOP_A, D1m))
            .addEgressPaths(walk(STOP_H, D1m))
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
            "Walk 1m ~ A ~ BUS R1_1 0:10 0:18 ~ B ~ BUS R2 0:20 0:31 ~ F ~ BUS R3_2 0:35 0:40 ~ H ~ Walk 1m [0:09 0:41 32m]",
            PathUtils.pathsToString(response)
    );
  }

  @Test
  public void noWaitStd() {
    requestBuilder.profile(NO_WAIT_STD);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: Board at stop B (the latest departure time) and arrive at E (the earliest arrival time)
    assertEquals(EXP_PATH_NO_WAIT, PathUtils.pathsToString(response));
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
            "Walk 1m ~ A ~ BUS R1_2 0:14 0:18 ~ C ~ BUS R2 0:21 0:32 ~ G ~ BUS R3_3 0:35 0:44 ~ H ~ Walk 1m [0:13 0:45 32m]",
            PathUtils.pathsToString(response)
    );
  }

  @Test
  public void noWaitStdReverse() {
    requestBuilder.profile(NO_WAIT_STD).searchDirection(REVERSE);

    var response = raptorService.route(requestBuilder.build(), data);

    // Searching in reverse, the boarding happens at the end, and the alighting is in the beginning:
    // expect: Board at stop E (the latest departure time) and arrive at B (the earliest arrival time)
    assertEquals(EXP_PATH_NO_WAIT, PathUtils.pathsToString(response));
  }

  @Test
  public void multiCriteria() {
    requestBuilder.profile(MULTI_CRITERIA);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: The multi-criteria search should find the best alternative because it looks at
    //         the arrival-time, generalized-cost, and departure-time(travel-time).
    assertEquals(EXP_PATH_MC, PathUtils.pathsToString(response));
  }
}