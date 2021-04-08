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
 * Raptor should return the optimal path with various egress paths. All Raptor
 * optimizations(McRaptor, Standard and Reverse Standard) should be tested.
 */
public class B02_EgressTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(RaptorConfig.defaultConfigForTest());

  @Before
  public void setup() {
    data.add(
        route("R1", STOP_B, STOP_C, STOP_D, STOP_E, STOP_F, STOP_G)
            .withTimetable(
                schedule("0:10, 0:12, 0:14, 0:16, 0:18, 0:20")
            )
    );

    requestBuilder.searchParams()
        .addAccessPaths(walk(STOP_B, D20s))
        .addEgressPaths(
            walk(STOP_D, D7m),    // Not optimal
            walk(STOP_E, D3m),    // Earliest arrival time: 0:16 + 3m = 0:19
            walk(STOP_F, D2m),    // Best compromise of cost and time
            walk(STOP_G, D1s)     // Lowest cost
        )
        .earliestDepartureTime(T00_00)
        .latestArrivalTime(T00_30)
    ;

    // Enable Raptor debugging by configuring the requestBuilder
    // data.debugToStdErr(requestBuilder);
  }

  @Test
  public void standard() {
    requestBuilder.profile(STANDARD);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: one path with the latest departure time.
    assertEquals(
        "Walk 20s ~ 2 ~ BUS R1 0:10 0:16 ~ 5 ~ Walk 3m [00:09:40 00:19:00 9m20s]",
        PathUtils.pathsToString(response)
    );
  }

  @Test
  public void standardReverse() {
    requestBuilder
        .profile(STANDARD)
        .searchDirection(REVERSE);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: one path with the latest departure time, same as found in the forward search.
    assertEquals(
        "Walk 20s ~ 2 ~ BUS R1 0:10 0:16 ~ 5 ~ Walk 3m [00:09:40 00:19:00 9m20s]",
        PathUtils.pathsToString(response)
    );
  }

  @Test
  public void multiCriteria() {
    requestBuilder.profile(MULTI_CRITERIA).searchParams();

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: All pareto optimal paths
    assertEquals(""
            + "Walk 20s ~ 2 ~ BUS R1 0:10 0:16 ~ 5 ~ Walk 3m [00:09:40 00:19:00 9m20s, cost: 1760]\n"
            + "Walk 20s ~ 2 ~ BUS R1 0:10 0:18 ~ 6 ~ Walk 2m [00:09:40 00:20:00 10m20s, cost: 1640]\n"
            + "Walk 20s ~ 2 ~ BUS R1 0:10 0:20 ~ 7 ~ Walk 1s [00:09:40 00:20:01 10m21s, cost: 1284]",
        PathUtils.pathsToString(response)
    );
  }
}