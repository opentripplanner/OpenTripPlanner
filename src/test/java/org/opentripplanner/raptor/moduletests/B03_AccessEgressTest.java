package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.join;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.raptor.api.request.RaptorProfile.STANDARD;
import static org.opentripplanner.raptor.spi.SearchDirection.REVERSE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.PathUtils;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.configure.RaptorConfig;

/**
 * FEATURE UNDER TEST
 * <p>
 * Raptor should return the optimal path with various access and egress paths. The Standard and
 * Standard Reverse optimizations should return the best time option, while the Multi-criteria
 * optimization should return a pareto optimal solutions with/without timetable view enabled.
 */
public class B03_AccessEgressTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data.withRoute(
      route("R1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E, STOP_F, STOP_G, STOP_H)
        .withTimetable(schedule("0:10, 0:14, 0:18, 0:22, 0:28, 0:32, 0:36, 0:40"))
    );

    requestBuilder
      .searchParams()
      .addAccessPaths(
        TestAccessEgress.walk(STOP_A, D1s), // Lowest cost
        TestAccessEgress.walk(STOP_B, D4m), // Best compromise of cost and time
        TestAccessEgress.walk(STOP_C, D7m), // Latest departure time: 0:16 - 5m = 0:11
        TestAccessEgress.walk(STOP_D, D20m) // Not optimal
      )
      .addEgressPaths(
        TestAccessEgress.walk(STOP_E, D20m), // Not optimal
        TestAccessEgress.walk(STOP_F, D7m), // Earliest arrival time: 0:18 + 3m = 0:21
        TestAccessEgress.walk(STOP_G, D4m), // Best compromise of cost and time
        TestAccessEgress.walk(STOP_H, D1s) // Lowest cost
      )
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T01_00);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void standard() {
    requestBuilder.profile(STANDARD);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: The latest departure combined with the earliest arrival is expected
    assertEquals(
      "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m 0tx]",
      PathUtils.pathsToString(response)
    );
  }

  @Test
  public void standardReverse() {
    requestBuilder.profile(STANDARD).searchDirection(REVERSE);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: The latest departure combined with the earliest arrival is expected - same as
    //         the forward search above
    assertEquals(
      "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m 0tx]",
      PathUtils.pathsToString(response)
    );
  }

  @Test
  public void multiCriteriaWithTimetable() {
    requestBuilder.profile(MULTI_CRITERIA).searchParams().timetableEnabled(true);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: With 3 optimal solutions for both access and egress we get 3 x 3 = 9 optimal
    //         alternatives then timetable is enabled.
    assertEquals(
      join(
        "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m 0tx $3120]",
        "Walk 4m ~ B ~ BUS R1 0:14 0:32 ~ F ~ Walk 7m [0:10 0:39 29m 0tx $3000]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:32 ~ F ~ Walk 7m [0:09:59 0:39 29m1s 0tx $2762]",
        "Walk 7m ~ C ~ BUS R1 0:18 0:36 ~ G ~ Walk 4m [0:11 0:40 29m 0tx $3000]",
        "Walk 4m ~ B ~ BUS R1 0:14 0:36 ~ G ~ Walk 4m [0:10 0:40 30m 0tx $2880]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:36 ~ G ~ Walk 4m [0:09:59 0:40 30m1s 0tx $2642]",
        "Walk 7m ~ C ~ BUS R1 0:18 0:40 ~ H ~ Walk 1s [0:11 0:40:01 29m1s 0tx $2762]",
        "Walk 4m ~ B ~ BUS R1 0:14 0:40 ~ H ~ Walk 1s [0:10 0:40:01 30m1s 0tx $2642]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:40 ~ H ~ Walk 1s [0:09:59 0:40:01 30m2s 0tx $2404]"
      ),
      PathUtils.pathsToString(response)
    );
  }

  /**
   * This test turn timetable "off", this is the same as {@code arriveBy=false}. There is no support
   * for {@code arriveBy=true}, which would prioritize the latest arrival if cost is the same.
   */
  @Test
  public void multiCriteriaWithoutTimetable() {
    requestBuilder.profile(MULTI_CRITERIA).searchParams().timetableEnabled(false);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: Expect pareto optimal results with earliest-arrival-time and cost as the criteria,
    //         but not departure-time.
    assertEquals(
      join(
        "Walk 7m ~ C ~ BUS R1 0:18 0:32 ~ F ~ Walk 7m [0:11 0:39 28m 0tx $3120]",
        "Walk 4m ~ B ~ BUS R1 0:14 0:32 ~ F ~ Walk 7m [0:10 0:39 29m 0tx $3000]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:32 ~ F ~ Walk 7m [0:09:59 0:39 29m1s 0tx $2762]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:36 ~ G ~ Walk 4m [0:09:59 0:40 30m1s 0tx $2642]",
        "Walk 1s ~ A ~ BUS R1 0:10 0:40 ~ H ~ Walk 1s [0:09:59 0:40:01 30m2s 0tx $2404]"
      ),
      PathUtils.pathsToString(response)
    );
  }
}
