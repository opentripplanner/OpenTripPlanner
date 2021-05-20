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
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.STOP_E;
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
 * <p>
 * Raptor should return the path that does not have any banned stops for multiple banned stops even
 * if it is the slowest path.
 */
public class D03_MultipleBannedStopsHardTest {

    private final TestTransitData data = new TestTransitData();
    private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
            new RaptorRequestBuilder<>();
    private final RaptorService<TestTripSchedule> raptorService =
            new RaptorService<>(RaptorConfig.defaultConfigForTest());


    /**
     * The expected result is tha same for all tests
     */
    private static final String EXPECTED_RESULT
            = "Walk 10s ~ 1 ~ BUS R3 0:03:30 0:05:40 ~ 3 ~ Walk 20s [00:02:50 00:06:10 3m20s]";

    /**
     * Stop on route (stop indexes): R1:  1 - 2 - 3 R2:  1 - 4 - 3 R3:  1 - 5 - 3
     * <p>
     * Schedule: R1: 00:01:35, 00:02:11, 00:03:11 R2: 00:02:30, 00:03:50, 00:04:50 R3: 00:03:30,
     * 00:04:50, 00:05:50
     * <p>
     * Hard Banned Stops: 2,4
     */
    @Before
    public void setup() {
        requestBuilder.slackProvider(
                defaultSlackProvider(D1m, D30s, D10s)
        );
        data.add(
                route(pattern("R1", STOP_A, STOP_B, STOP_C))
                        .withTimetable(schedule().departures("00:01:35, 00:02:11, 00:03:11")
                                .arrDepOffset(D10s))
        );
        data.add(
                route(pattern("R2", STOP_A, STOP_D, STOP_C))
                        .withTimetable(schedule().departures("00:02:30, 00:03:50, 00:04:50")
                                .arrDepOffset(D10s))
        );
        data.add(
                route(pattern("R3", STOP_A, STOP_E, STOP_C))
                        .withTimetable(schedule().departures("00:03:30, 00:04:50, 00:05:50")
                                .arrDepOffset(D10s))
        );

        data.add(STOP_B).add(STOP_D);

        requestBuilder.searchParams()
                .addAccessPaths(walk(STOP_A, D10s))
                .addEgressPaths(walk(STOP_C, D20s))
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
                EXPECTED_RESULT.replace("]", ", cost: 890]"),
                pathsToString(response)
        );

    }
}
