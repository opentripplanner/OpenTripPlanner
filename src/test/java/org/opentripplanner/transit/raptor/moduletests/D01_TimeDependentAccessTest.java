package org.opentripplanner.transit.raptor.moduletests;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.transit.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider.defaultSlackProvider;
import static org.opentripplanner.util.time.TimeUtils.hm2time;

import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;

/*
 * FEATURE UNDER TEST
 *
 * Raptor should take into account time restrictions on access. If the time restrictions require it,
 * there should be a wait before boarding the trip so that the access is traversed while "open".
 */
public class D01_TimeDependentAccessTest implements RaptorTestConstants {

    private final TestTransitData data = new TestTransitData();
    private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
            new RaptorRequestBuilder<>();
    private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
            RaptorConfig.defaultConfigForTest()
    );

    @Before
    public void setup() {
        data.add(
                route("R1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E)
                        .withTimetable(
                                schedule("0:10 0:15 0:20 0:25 0:30")
                        )
        );
        data.add(
                route("R2", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E)
                        .withTimetable(
                                schedule("0:15 0:20 0:25 0:30 0:35")
                        )
        );
        data.add(
                route("R3", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E)
                        .withTimetable(
                                schedule("0:20 0:25 0:30 0:35 0:40")
                        )
        );
        data.add(
                route("R4", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E)
                        .withTimetable(
                                schedule("0:25 0:30 0:35 0:40 0:45")
                        )
        );
        requestBuilder.searchParams()
                .addEgressPaths(walk(STOP_E, D1m));

        requestBuilder.searchParams()
                .earliestDepartureTime(T00_10)
                .searchWindow(Duration.ofMinutes(30))
                .timetableEnabled(true);

        // We will test board- and alight-slack in a separate test
        requestBuilder.slackProvider(
                defaultSlackProvider(60, 0, 0)
        );

        // Enable Raptor debugging by configuring the requestBuilder
        // data.debugToStdErr(requestBuilder);
    }

    /*
     * There is no time restriction, all routes are found.
     */
    @Test
    public void openInWholeSearchIntervalTest() {
        requestBuilder.profile(RaptorProfile.MULTI_CRITERIA)
                .searchParams()
                .addAccessPaths(
                        new TimeDependentAccessEgress(STOP_B, D2m, null, T00_00, T01_00)
                );

        var response = raptorService.route(requestBuilder.build(), data);

        assertEquals(
                ""
                        + "Walk 2m ~ 2 ~ BUS R1 0:15 0:30 ~ 5 ~ Walk 1m [00:13:00 00:31:00 18m, cost: 2220]\n"
                        + "Walk 2m ~ 2 ~ BUS R2 0:20 0:35 ~ 5 ~ Walk 1m [00:18:00 00:36:00 18m, cost: 2220]\n"
                        + "Walk 2m ~ 2 ~ BUS R3 0:25 0:40 ~ 5 ~ Walk 1m [00:23:00 00:41:00 18m, cost: 2220]\n"
                        + "Walk 2m ~ 2 ~ BUS R4 0:30 0:45 ~ 5 ~ Walk 1m [00:28:00 00:46:00 18m, cost: 2220]",
                pathsToString(response)
        );
    }

    /*
     * The access is only open after 00:18, which means that we may arrive at the stop at 00:20 at
     * the earliest. Due to this:
     *   - boarding R1 is not possible
     */
    @Test
    public void openInSecondHalfIntervalTest() {
        requestBuilder.profile(RaptorProfile.MULTI_CRITERIA)
                .searchParams()
                .addAccessPaths(
                        new TimeDependentAccessEgress(STOP_B, D2m, null, hm2time(0, 18), T01_00)
                );

        var response = raptorService.route(requestBuilder.build(), data);

        assertEquals(
                ""
                        + "Walk 2m ~ 2 ~ BUS R2 0:20 0:35 ~ 5 ~ Walk 1m [00:18:00 00:36:00 18m, cost: 2220]\n"
                        + "Walk 2m ~ 2 ~ BUS R3 0:25 0:40 ~ 5 ~ Walk 1m [00:23:00 00:41:00 18m, cost: 2220]\n"
                        + "Walk 2m ~ 2 ~ BUS R4 0:30 0:45 ~ 5 ~ Walk 1m [00:28:00 00:46:00 18m, cost: 2220]",
                pathsToString(response)
        );
    }

    /*
     * The access is only open before 00:20, which means that we arrive at the stop by 00:22. Due to this:
     *   - there needs to be a wait of 3 minutes for R3
     *   - R4 is discarded since it is not better than taking R3
     */
    @Test
    public void openInFirstHalfIntervalTest() {
        requestBuilder.profile(RaptorProfile.MULTI_CRITERIA)
                .searchParams()
                .addAccessPaths(
                        new TimeDependentAccessEgress(STOP_B, D2m, null, T00_00, hm2time(0, 20))
                );

        var response = raptorService.route(requestBuilder.build(), data);

        assertEquals(
                ""
                        + "Walk 2m ~ 2 ~ BUS R1 0:15 0:30 ~ 5 ~ Walk 1m [00:13:00 00:31:00 18m, cost: 2220]\n"
                        + "Walk 2m ~ 2 ~ BUS R2 0:20 0:35 ~ 5 ~ Walk 1m [00:18:00 00:36:00 18m, cost: 2220]\n"
                        + "Walk 2m ~ 2 ~ BUS R3 0:25 0:40 ~ 5 ~ Walk 1m [00:20:00 00:41:00 21m, cost: 2400]",
                pathsToString(response)
        );
    }

    /*
     * The access is only open after 00:18 and before 00:20. This means that we arrive at the stop at
     * 00:20 at the earliest and 00:22 at the latest. Due to this:
     *   - boarding R1 is not possible
     *   - there needs to be a wait of 3 minutes for R3
     *   - R4 is discarded since it is not better than taking R3
     */
    @Test
    public void partiallyOpenIntervalTest() {
        requestBuilder.profile(RaptorProfile.MULTI_CRITERIA)
                .searchParams()
                .addAccessPaths(
                        new TimeDependentAccessEgress(
                                STOP_B, D2m, null, hm2time(0, 18), hm2time(0, 20))
                );

        var response = raptorService.route(requestBuilder.build(), data);

        assertEquals(
                ""
                        + "Walk 2m ~ 2 ~ BUS R2 0:20 0:35 ~ 5 ~ Walk 1m [00:18:00 00:36:00 18m, cost: 2220]\n"
                        + "Walk 2m ~ 2 ~ BUS R3 0:25 0:40 ~ 5 ~ Walk 1m [00:20:00 00:41:00 21m, cost: 2400]",
                pathsToString(response)
        );
    }

    private static class TimeDependentAccessEgress extends AccessEgress {

        private final int opening;
        private final int closing;

        public TimeDependentAccessEgress(
                int toFromStop,
                int durationInSeconds,
                State lastState,
                int opening,
                int closing
        ) {
            super(toFromStop, durationInSeconds, lastState);
            this.opening = opening;
            this.closing = closing;
        }

        @Override
        public int earliestDepartureTime(int requestedDepartureTime) {
            if (requestedDepartureTime < opening) {
                return opening;
            } else if (requestedDepartureTime > closing) {
                // return the opening time for the next day
                return opening + 24 * 3600;
            }
            return requestedDepartureTime;
        }

        @Override
        public int latestArrivalTime(int requestedArrivalTime) {
            // opening & closing is relative to the departure
            int requestedDepartureTime = requestedArrivalTime - durationInSeconds();
            int closeAtArrival = closing + durationInSeconds();

            if (requestedDepartureTime < opening) {
                // return the closing hours for the previous day, offset with durationInSeconds()
                return closeAtArrival - 24 * 3600;
            }
            else if (requestedArrivalTime > closeAtArrival) {
                return closeAtArrival;
            }
            return requestedArrivalTime;
        }
    }
}
