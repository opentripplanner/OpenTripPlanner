package org.opentripplanner.transit.raptor.moduletests;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.STANDARD;
import static org.opentripplanner.transit.raptor.api.request.SearchDirection.REVERSE;

import org.junit.Before;
import org.junit.Test;
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

  @Before
  public void setup() {
    data.withRoute(
        route("R1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E, STOP_F, STOP_G, STOP_H)
            .withTimetable(
                schedule("0:10, 0:12, 0:14, 0:16, 0:18, 0:20, 0:22, 0:24")
            )
    );

    requestBuilder.searchParams()
        .addAccessPaths(
            walk(STOP_A, D1s),    // Lowest cost
            walk(STOP_B, D2m),    // Best compromise of cost and time
            walk(STOP_C, D3m),    // Latest departure time: 0:14 - 3m = 0:11
            walk(STOP_D, D7m)     // Not optimal
        )
        .addEgressPaths(
            walk(STOP_E, D7m),    // Not optimal
            walk(STOP_F, D3m),    // Earliest arrival time: 0:18 + 3m = 0:21
            walk(STOP_G, D2m),    // Best compromise of cost and time
            walk(STOP_H, D1s)     // Lowest cost
        )
        .earliestDepartureTime(T00_00)
        .latestArrivalTime(T00_30)
    ;

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void standard() {
    requestBuilder.profile(STANDARD);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: The latest departure combined with the earliest arrival is expected
    assertEquals(
        "Walk 3m ~ 3 ~ BUS R1 0:14 0:20 ~ 6 ~ Walk 3m [0:11 0:23 12m]",
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
        "Walk 3m ~ 3 ~ BUS R1 0:14 0:20 ~ 6 ~ Walk 3m [0:11 0:23 12m]",
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
            + "Walk 3m ~ 3 ~ BUS R1 0:14 0:20 ~ 6 ~ Walk 3m [0:11 0:23 12m $2400]\n"
            + "Walk 2m ~ 2 ~ BUS R1 0:12 0:20 ~ 6 ~ Walk 3m [0:10 0:23 13m $2280]\n"
            + "Walk 1s ~ 1 ~ BUS R1 0:10 0:20 ~ 6 ~ Walk 3m [0:09:59 0:23 13m1s $1924]\n"
            + "Walk 3m ~ 3 ~ BUS R1 0:14 0:22 ~ 7 ~ Walk 2m [0:11 0:24 13m $2280]\n"
            + "Walk 2m ~ 2 ~ BUS R1 0:12 0:22 ~ 7 ~ Walk 2m [0:10 0:24 14m $2160]\n"
            + "Walk 1s ~ 1 ~ BUS R1 0:10 0:22 ~ 7 ~ Walk 2m [0:09:59 0:24 14m1s $1804]\n"
            + "Walk 3m ~ 3 ~ BUS R1 0:14 0:24 ~ 8 ~ Walk 1s [0:11 0:24:01 13m1s $1924]\n"
            + "Walk 2m ~ 2 ~ BUS R1 0:12 0:24 ~ 8 ~ Walk 1s [0:10 0:24:01 14m1s $1804]\n"
            + "Walk 1s ~ 1 ~ BUS R1 0:10 0:24 ~ 8 ~ Walk 1s [0:09:59 0:24:01 14m2s $1448]",
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
            + "Walk 3m ~ 3 ~ BUS R1 0:14 0:20 ~ 6 ~ Walk 3m [0:11 0:23 12m $2400]\n"
            + "Walk 2m ~ 2 ~ BUS R1 0:12 0:20 ~ 6 ~ Walk 3m [0:10 0:23 13m $2280]\n"
            + "Walk 1s ~ 1 ~ BUS R1 0:10 0:20 ~ 6 ~ Walk 3m [0:09:59 0:23 13m1s $1924]\n"
            + "Walk 1s ~ 1 ~ BUS R1 0:10 0:22 ~ 7 ~ Walk 2m [0:09:59 0:24 14m1s $1804]\n"
            + "Walk 1s ~ 1 ~ BUS R1 0:10 0:24 ~ 8 ~ Walk 1s [0:09:59 0:24:01 14m2s $1448]",
        PathUtils.pathsToString(response)
    );
  }
}