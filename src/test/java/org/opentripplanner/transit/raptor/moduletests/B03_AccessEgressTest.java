package org.opentripplanner.transit.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
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
 * Raptor should return the optimal path with various access and egress paths. The
 * Standard and Standard Reverse optimizations should return the best time option,
 * while the Multi-criteria optimization should return a pareto optimal solutions
 * with/without timetable view enabled.
 */
public class B03_AccessEgressTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(RaptorConfig.defaultConfigForTest());

  @BeforeEach
  public void setup() {
    data.withRoute(
        route("R1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E, STOP_F, STOP_G, STOP_H)
            .withTimetable(
                schedule("0:10, 0:14, 0:18, 0:22, 0:28, 0:32, 0:36, 0:40")
            )
    );

    requestBuilder.searchParams()
        .addAccessPaths(
            walk(STOP_A, D1s),  // Lowest cost
            walk(STOP_B, D4m),  // Best compromise of cost and time
            walk(STOP_C, D7m),  // Latest departure time: 0:16 - 5m = 0:11
            walk(STOP_D, D20m)   // Not optimal
        )
        .addEgressPaths(
            walk(STOP_E, D20m),  // Not optimal
            walk(STOP_F, D7m),  // Earliest arrival time: 0:18 + 3m = 0:21
            walk(STOP_G, D4m),  // Best compromise of cost and time
            walk(STOP_H, D1s)   // Lowest cost
        )
        .earliestDepartureTime(T00_00)
        .latestArrivalTime(T01_00)
    ;

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void standard() {
    requestBuilder.profile(STANDARD);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: The latest departure combined with the earliest arrival is expected
    assertEquals(
        "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m]",
        PathUtils.pathsToString(response)
    );
  }

  @Test
  public void standardReverse() {
    requestBuilder
        .profile(STANDARD)
        .searchDirection(REVERSE);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: The latest departure combined with the earliest arrival is expected - same as
    //         the forward search above
    assertEquals(
        "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m]",
        PathUtils.pathsToString(response)
    );
  }

  @Test
  public void multiCriteriaWithTimetable() {
    requestBuilder.profile(MULTI_CRITERIA)
        .searchParams().timetableEnabled(true);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: With 3 optimal solutions for both access and egress we get 3 x 3 = 9 optimal
    //         alternatives then timetable is enabled.
    assertEquals(""
            + "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m $3120]\n"
            + "Walk 4m ~ B ~ BUS R1 0:14 0:32 ~ F ~ Walk 7m [0:10 0:39 29m $3000]\n"
            + "Walk 1s ~ A ~ BUS R1 0:10 0:32 ~ F ~ Walk 7m [0:09:59 0:39 29m1s $2762]\n"
            + "Walk 7m ~ C ~ BUS R1 0:18 0:36 ~ G ~ Walk 4m [0:11 0:40 29m $3000]\n"
            + "Walk 4m ~ B ~ BUS R1 0:14 0:36 ~ G ~ Walk 4m [0:10 0:40 30m $2880]\n"
            + "Walk 1s ~ A ~ BUS R1 0:10 0:36 ~ G ~ Walk 4m [0:09:59 0:40 30m1s $2642]\n"
            + "Walk 7m ~ C ~ BUS R1 0:18 0:40 ~ H ~ Walk 1s [0:11 0:40:01 29m1s $2762]\n"
            + "Walk 4m ~ B ~ BUS R1 0:14 0:40 ~ H ~ Walk 1s [0:10 0:40:01 30m1s $2642]\n"
            + "Walk 1s ~ A ~ BUS R1 0:10 0:40 ~ H ~ Walk 1s [0:09:59 0:40:01 30m2s $2404]",
        PathUtils.pathsToString(response)
    );
  }

  /**
   * This test turn timetable "off", this is the same as {@code arriveBy=false}. There is no
   * support for {@code arriveBy=true}, witch would prioritize the latest arrival if cost is
   * the same.
   */
  @Test
  public void multiCriteriaWithoutTimetable() {
    requestBuilder.profile(MULTI_CRITERIA)
        .searchParams().timetableEnabled(false);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: Expect pareto optimal results with earliest-arrival-time and cost as the criteria,
    //         but not departure-time.
    assertEquals(""
            + "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m $3120]\n"
            + "Walk 4m ~ B ~ BUS R1 0:14 0:32 ~ F ~ Walk 7m [0:10 0:39 29m $3000]\n"
            + "Walk 1s ~ A ~ BUS R1 0:10 0:32 ~ F ~ Walk 7m [0:09:59 0:39 29m1s $2762]\n"
            + "Walk 1s ~ A ~ BUS R1 0:10 0:36 ~ G ~ Walk 4m [0:09:59 0:40 30m1s $2642]\n"
            + "Walk 1s ~ A ~ BUS R1 0:10 0:40 ~ H ~ Walk 1s [0:09:59 0:40:01 30m2s $2404]",
        PathUtils.pathsToString(response)
    );
  }
}