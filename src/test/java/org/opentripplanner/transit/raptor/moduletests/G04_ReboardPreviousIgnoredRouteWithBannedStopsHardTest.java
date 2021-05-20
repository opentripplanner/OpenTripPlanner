package org.opentripplanner.transit.raptor.moduletests;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.request.SearchDirection;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.*;
import static org.opentripplanner.transit.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider.defaultSlackProvider;

/**
 * FEATURE UNDER TEST
 *
 * Raptor should return a path that may have included a route which had a banned stop earlier
 * if it is able to to re-board that route again later down the line through a different route.
 */
public class G04_ReboardPreviousIgnoredRouteWithBannedStopsHardTest {

  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(RaptorConfig.defaultConfigForTest());


  /**
   * The expected result is tha same for all tests
   */
  private static final String EXPECTED_RESULT
          = "Walk 10s ~ 1 ~ BUS R1 0:01:35 0:03:01 ~ 3 ~ "
          + "BUS R2 0:04:41 0:05:01 ~ 4 ~ Walk 20s [0:00:55 0:05:31 4m36s]";

  /**
   *
   * Network:
   *        2
   *    /     \
   *   1 - 8 - 3 - 4
   *
   * Stop on route (stop indexes):
   *   R1:  1 - 2 - 3
   *   R2: 1 - 8 - 3 - 4
   *
   * Schedule:
   *   R1: 00:01:35, 00:02:11, 00:03:11
   *   R2: 00:01:11,00:03:41, 00:04:41, 00:05:11
   *
   * Hard Banned Stops:
   *   8
   */
  @Before
  public void setup() {
    requestBuilder.slackProvider(
            defaultSlackProvider(D1m, D30s, D10s)
    );
    data.withRoute(
            route(pattern("R1",STOP_A,STOP_B, STOP_C))
                    .withTimetable(schedule().departures("00:01:35, 00:02:11, 00:03:11").arrDepOffset(D10s))
    );
    data.withRoute(
            route(pattern("R2",STOP_A, STOP_H, STOP_C, STOP_D))
                    .withTimetable(
                            schedule().arrivals("00:01:00, 00:01:30, 00:02:41, 00:05:01") //Arrival at Stop C earlier then R1
                                    .departures("00:01:11, 00:01:41, 00:04:41, 00:05:11") //Departure at Stop C later then R1 to allow reboarding
                    )
    );

    data.withBannedStop(STOP_H);


    requestBuilder.searchParams()
                  .addAccessPaths(walk(STOP_A, D10s))
                  .addEgressPaths(walk(STOP_D, D20s))
                  .earliestDepartureTime(T00_00)
                  .latestArrivalTime(T00_30)
                  .searchWindowInSeconds(D1m)
    ;

//     Enable Raptor debugging by configuring the requestBuilder
//     data.debugToStdErr(requestBuilder);
  }

  @Test
  public void standard() {
    var request = requestBuilder
            .profile(RaptorProfile.STANDARD)
            .build();

    var response = raptorService.route(request, data);

    assertEquals(
            EXPECTED_RESULT,
            pathsToString(response)
    );
  }

  @Test
  public void standardReverse() {
    var request = requestBuilder
            .searchDirection(SearchDirection.REVERSE)
            .profile(RaptorProfile.STANDARD)
            .build();

    var response = raptorService.route(request, data);

    assertEquals(
            EXPECTED_RESULT,
            pathsToString(response)
    );
  }

  @Test
  public void multiCriteria() {
    var request = requestBuilder
            .profile(RaptorProfile.MULTI_CRITERIA)
            .build();

    var response = raptorService.route(request, data);

    assertEquals(
            EXPECTED_RESULT.replace("]", " $1566]"),
            pathsToString(response)
    );

    }

}
