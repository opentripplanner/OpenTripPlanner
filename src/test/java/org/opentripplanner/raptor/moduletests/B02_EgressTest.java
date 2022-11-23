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
 * Raptor should return the optimal path with various egress paths. All Raptor
 * optimizations(McRaptor, Standard and Reverse Standard) should be tested.
 */
public class B02_EgressTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    data.withRoute(
      route("R1", STOP_B, STOP_C, STOP_D, STOP_E, STOP_F, STOP_G)
        .withTimetable(schedule("0:10, 0:14, 0:18, 0:20, 0:24, 0:28"))
    );

    requestBuilder
      .searchParams()
      .addAccessPaths(TestAccessEgress.walk(STOP_B, D20s))
      .addEgressPaths(
        TestAccessEgress.walk(STOP_D, D20m), // Not optimal
        TestAccessEgress.walk(STOP_E, D7m), // Earliest arrival time: 0:16 + 3m = 0:19
        TestAccessEgress.walk(STOP_F, D4m), // Best compromise of cost and time
        TestAccessEgress.walk(STOP_G, D1s) // Lowest cost
      )
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_30);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void standard() {
    requestBuilder.profile(STANDARD);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: one path with the latest departure time.
    assertEquals(
      "Walk 20s ~ B ~ BUS R1 0:10 0:20 ~ E ~ Walk 7m [0:09:40 0:27 17m20s 0tx]",
      PathUtils.pathsToString(response)
    );
  }

  @Test
  public void standardReverse() {
    requestBuilder.profile(STANDARD).searchDirection(REVERSE);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: one path with the latest departure time, same as found in the forward search.
    assertEquals(
      "Walk 20s ~ B ~ BUS R1 0:10 0:20 ~ E ~ Walk 7m [0:09:40 0:27 17m20s 0tx]",
      PathUtils.pathsToString(response)
    );
  }

  @Test
  public void multiCriteria() {
    requestBuilder.profile(MULTI_CRITERIA).searchParams();

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: All pareto optimal paths
    assertEquals(
      join(
        "Walk 20s ~ B ~ BUS R1 0:10 0:20 ~ E ~ Walk 7m [0:09:40 0:27 17m20s 0tx $2080]",
        "Walk 20s ~ B ~ BUS R1 0:10 0:24 ~ F ~ Walk 4m [0:09:40 0:28 18m20s 0tx $1960]",
        "Walk 20s ~ B ~ BUS R1 0:10 0:28 ~ G ~ Walk 1s [0:09:40 0:28:01 18m21s 0tx $1722]"
      ),
      PathUtils.pathsToString(response)
    );
  }
}
