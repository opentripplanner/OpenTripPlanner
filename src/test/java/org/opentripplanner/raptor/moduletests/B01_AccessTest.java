package org.opentripplanner.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.api.PathUtils.join;
import static org.opentripplanner.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.raptor.api.request.RaptorProfile.STANDARD;
import static org.opentripplanner.raptor.spi.SearchDirection.REVERSE;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.time.DurationUtils;
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
 * Raptor should return the optimal path with various access paths. All Raptor
 * optimizations(McRaptor, Standard and Reverse Standard) should be tested.
 */
public class B01_AccessTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data.withRoute(
      route("R1", STOP_B, STOP_C, STOP_D, STOP_E, STOP_F)
        .withTimetable(schedule("0:10 0:14 0:18 0:22 0:25"))
    );

    requestBuilder
      .searchParams()
      .addAccessPaths(
        TestAccessEgress.walk(STOP_B, D1s), // Lowest cost
        TestAccessEgress.walk(STOP_C, D4m), // Best compromise of cost and time
        TestAccessEgress.walk(STOP_D, DurationUtils.durationInSeconds("7m")), // Latest departure time
        TestAccessEgress.walk(STOP_E, DurationUtils.durationInSeconds("13m")) // Not optimal
      )
      .addEgressPaths(TestAccessEgress.walk(STOP_F, D1s))
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30)
      // Removing the search-window should not have any effect, but it does.
      .searchWindow(Duration.ofMinutes(20));

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void standard() {
    requestBuilder.profile(STANDARD);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: one path with the latest departure time.
    assertEquals(
      "Walk 7m ~ D ~ BUS R1 0:18 0:25 ~ F ~ Walk 1s [0:11 0:25:01 14m1s 0tx]",
      PathUtils.pathsToString(response)
    );
  }

  @Test
  public void standardReverse() {
    requestBuilder.profile(STANDARD).searchDirection(REVERSE);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: one path with the latest departure time, same as found in the forward search.
    assertEquals(
      "Walk 7m ~ D ~ BUS R1 0:18 0:25 ~ F ~ Walk 1s [0:11 0:25:01 14m1s 0tx]",
      PathUtils.pathsToString(response)
    );
  }

  @Test
  public void multiCriteria() {
    requestBuilder.profile(MULTI_CRITERIA).searchParams().timetableEnabled(true);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: All pareto optimal paths
    assertEquals(
      join(
        "Walk 7m 0:11 0:18 $840 ~ D 0s ~ BUS R1 0:18 0:25 7m $1020 ~ F 0s ~ Walk 1s 0:25 0:25:01 $2 [0:11 0:25:01 14m1s 0tx $1862]",
        "Walk 4m 0:10 0:14 $480 ~ C 0s ~ BUS R1 0:14 0:25 11m $1260 ~ F 0s ~ Walk 1s 0:25 0:25:01 $2 [0:10 0:25:01 15m1s 0tx $1742]",
        "Walk 1s 0:09:59 0:10 $2 ~ B 0s ~ BUS R1 0:10 0:25 15m $1500 ~ F 0s ~ Walk 1s 0:25 0:25:01 $2 [0:09:59 0:25:01 15m2s 0tx $1504]"
      ),
      PathUtils.pathsToStringDetailed(response)
    );
  }
}
