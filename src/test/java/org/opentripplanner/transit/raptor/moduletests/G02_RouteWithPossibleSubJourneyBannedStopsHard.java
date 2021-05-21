package org.opentripplanner.transit.raptor.moduletests;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.D10s;
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.D1m;
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.D20s;
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.D30s;
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.STOP_A;
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.STOP_B;
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.STOP_C;
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.STOP_D;
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.T00_00;
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.T00_30;
import static org.opentripplanner.transit.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider.defaultSlackProvider;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.request.SearchDirection;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;

/**
 * FEATURE UNDER TEST
 *
 * Raptor should be able to re-board a train later on the route that does not pass through the banned
 * stop even if earlier on their is a banned stop.
 */
public class G02_RouteWithPossibleSubJourneyBannedStopsHard {
    private final TestTransitData data = new TestTransitData();
    private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
    private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(RaptorConfig.defaultConfigForTest());


    /** The expected result is tha same for all tests */
    private static final String EXPECTED_RESULT
            = "Walk 10s ~ 1 ~ BUS R1 0:04:11 0:05:01 ~ 4 ~ Walk 20s [0:03:31 0:05:31 2m]";

    /**
     * Stop on route (stop indexes):
     *   R1:  1 - 2 - 3 - 1 - 4
     *
     * Schedule:
     *   R1: 00:01:35, 00:02:11, 00:03:11, 00:04:11, 00:05:11
     *
     * Hard Banned Stops:
     *    2
     */
    @Before
    public void setup() {
        requestBuilder.slackProvider(
                defaultSlackProvider(D1m, D30s, D10s)
        );
        data.withRoute(
                route(pattern("R1",STOP_A,STOP_B, STOP_C, STOP_A, STOP_D))
                        .withTimetable(schedule().departures("00:01:35, 00:02:11, 00:03:11, 00:04:11, 00:05:11").arrDepOffset(D10s))
        );

        data.withBannedStop(STOP_B);


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
                EXPECTED_RESULT.replace("]", " $810]"),
                pathsToString(response)
        );

    }
}
