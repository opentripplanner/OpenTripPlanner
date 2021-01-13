package org.opentripplanner.transit.raptor.moduletests;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor._data.api.PathUtils;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.STANDARD;
import static org.opentripplanner.transit.raptor.api.request.SearchDirection.REVERSE;

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
    data.add(
        route("R1", STOP_0, STOP_1, STOP_2, STOP_3, STOP_4, STOP_5, STOP_6, STOP_7)
            .withTimetable(
                schedule("0:10, 0:12, 0:14, 0:16, 0:18, 0:20, 0:22, 0:24")
            )
    );

    requestBuilder.searchParams()
        .addAccessPaths(
            walk(STOP_0, D1s),    // Lowest cost
            walk(STOP_1, D2m),    // Best compromise of cost and time
            walk(STOP_2, D3m),    // Latest departure time: 0:14 - 3m = 0:11
            walk(STOP_3, D7m)     // Not optimal
        )
        .addEgressPaths(
            walk(STOP_4, D7m),    // Not optimal
            walk(STOP_5, D3m),    // Earliest arrival time: 0:18 + 3m = 0:21
            walk(STOP_6, D2m),    // Best compromise of cost and time
            walk(STOP_7, D1s)     // Lowest cost
        )
        .earliestDepartureTime(T00_00)
        .latestArrivalTime(T00_30)
    ;

    // Enable Raptor debugging by configuring the requestBuilder
    // data.debugRaptorStateToSdtErr(requestBuilder);
  }

  @Test
  public void standard() {
    requestBuilder.profile(STANDARD);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: The latest departure combined with the earliest arrival is expected
    assertEquals(
        "Walk 3m ~ 2 ~ BUS R1 0:14 0:20 ~ 5 ~ Walk 3m [00:11:00 00:23:00 12m]",
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
        "Walk 3m ~ 2 ~ BUS R1 0:14 0:20 ~ 5 ~ Walk 3m [00:11:00 00:23:00 12m]",
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
            + "Walk 3m ~ 2 ~ BUS R1 0:14 0:20 ~ 5 ~ Walk 3m [00:11:00 00:23:00 12m, cost: 2400]\n"
            + "Walk 2m ~ 1 ~ BUS R1 0:12 0:20 ~ 5 ~ Walk 3m [00:10:00 00:23:00 13m, cost: 2280]\n"
            + "Walk 1s ~ 0 ~ BUS R1 0:10 0:20 ~ 5 ~ Walk 3m [00:09:59 00:23:00 13m1s, cost: 1924]\n"
            + "Walk 3m ~ 2 ~ BUS R1 0:14 0:22 ~ 6 ~ Walk 2m [00:11:00 00:24:00 13m, cost: 2280]\n"
            + "Walk 2m ~ 1 ~ BUS R1 0:12 0:22 ~ 6 ~ Walk 2m [00:10:00 00:24:00 14m, cost: 2160]\n"
            + "Walk 1s ~ 0 ~ BUS R1 0:10 0:22 ~ 6 ~ Walk 2m [00:09:59 00:24:00 14m1s, cost: 1804]\n"
            + "Walk 3m ~ 2 ~ BUS R1 0:14 0:24 ~ 7 ~ Walk 1s [00:11:00 00:24:01 13m1s, cost: 1924]\n"
            + "Walk 2m ~ 1 ~ BUS R1 0:12 0:24 ~ 7 ~ Walk 1s [00:10:00 00:24:01 14m1s, cost: 1804]\n"
            + "Walk 1s ~ 0 ~ BUS R1 0:10 0:24 ~ 7 ~ Walk 1s [00:09:59 00:24:01 14m2s, cost: 1448]",
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
            + "Walk 3m ~ 2 ~ BUS R1 0:14 0:20 ~ 5 ~ Walk 3m [00:11:00 00:23:00 12m, cost: 2400]\n"
            + "Walk 2m ~ 1 ~ BUS R1 0:12 0:20 ~ 5 ~ Walk 3m [00:10:00 00:23:00 13m, cost: 2280]\n"
            + "Walk 1s ~ 0 ~ BUS R1 0:10 0:20 ~ 5 ~ Walk 3m [00:09:59 00:23:00 13m1s, cost: 1924]\n"
            + "Walk 1s ~ 0 ~ BUS R1 0:10 0:22 ~ 6 ~ Walk 2m [00:09:59 00:24:00 14m1s, cost: 1804]\n"
            + "Walk 1s ~ 0 ~ BUS R1 0:10 0:24 ~ 7 ~ Walk 1s [00:09:59 00:24:01 14m2s, cost: 1448]",
        PathUtils.pathsToString(response)
    );
  }
}