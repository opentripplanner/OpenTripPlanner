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
 * Raptor should return the optimal path with various access paths. All Raptor
 * optimizations(McRaptor, Standard and Reverse Standard) should be tested.
 */
public class B01_AccessTest implements RaptorTestConstants {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(RaptorConfig.defaultConfigForTest());

  @Before
  public void setup() {
    data.withRoute(
        route("R1", STOP_B, STOP_C, STOP_D, STOP_E, STOP_F, STOP_G)
            .withTimetable(
                schedule("0:10, 0:12, 0:14, 0:16, 0:18, 0:20")
            )
    );

    requestBuilder.searchParams()
        .addAccessPaths(
            walk(STOP_B, D1s),    // Lowest cost
            walk(STOP_C, D2m),    // Best compromise of cost and time
            walk(STOP_D, D3m),    // Latest departure time
            walk(STOP_E, D7m)     // Not optimal
        )
        .addEgressPaths(
            walk(STOP_G, D1s)
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

    // expect: one path with the latest departure time.
    assertEquals(
        "Walk 3m ~ 4 ~ BUS R1 0:14 0:20 ~ 7 ~ Walk 1s [0:11 0:20:01 9m1s]",
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
        "Walk 3m ~ 4 ~ BUS R1 0:14 0:20 ~ 7 ~ Walk 1s [0:11 0:20:01 9m1s]",
        PathUtils.pathsToString(response)
    );
  }

  @Test
  public void multiCriteria() {
    requestBuilder.profile(MULTI_CRITERIA)
        .searchParams().timetableEnabled(true);

    var response = raptorService.route(requestBuilder.build(), data);

    // expect: All pareto optimal paths
    assertEquals(""
            + "Walk 3m ~ 4 ~ BUS R1 0:14 0:20 ~ 7 ~ Walk 1s [0:11 0:20:01 9m1s $1684]\n"
            + "Walk 2m ~ 3 ~ BUS R1 0:12 0:20 ~ 7 ~ Walk 1s [0:10 0:20:01 10m1s $1564]\n"
            + "Walk 1s ~ 2 ~ BUS R1 0:10 0:20 ~ 7 ~ Walk 1s [0:09:59 0:20:01 10m2s $1208]",
        PathUtils.pathsToString(response)
    );
  }
}