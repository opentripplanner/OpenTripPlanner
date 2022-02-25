package org.opentripplanner.transit.raptor.moduletests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.view.Heuristics;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;
import org.opentripplanner.transit.raptor.service.RangeRaptorDynamicSearch;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;

/**
 * Feature under test
 *
 * Raptor should return a proper array for heuristic values, i.e, time and transfers
 */
public class G01_HeuristicTest implements RaptorTestConstants {

    // Any big negative number will do, but -1 is a legal value
    private static final int UNREACHED = -9999;
    private static final int[] BEST_TRANSFERS = {UNREACHED, 1, 0, 0, -1};
    private static final int[] BEST_TIMES = {
            UNREACHED,
    //  Egress + R2     + Transfer + Slack + R1
            20 + 3 * 60 + 30       + 60    + 2 * 60,
            20 + 3 * 60 + 30,
            20 + 3 * 60,
            20
    };

    private final TestTransitData data = new TestTransitData();
    private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
    private final RaptorConfig<TestTripSchedule> config = RaptorConfig.defaultConfigForTest();

    /**
     * Stops: 0..4
     *
     * Stop on route (stop indexes):
     *   R1:  1 - 2
     *   R2:  3 - 4
     *
     * Schedule:
     *   R1: 00:01 - 00:03
     *   R2: 00:05 - 00:08
     *
     * Access (toStop & duration):
     *   1  30s
     *
     * Egress (fromStop & duration):
     *   3  20s
     *
     * Transfers:
     *   2 -> 3 30s
     */
    @BeforeEach
    public void setup() {
        data.withRoute(
                route(pattern("R1", STOP_A, STOP_B))
                        .withTimetable(schedule("00:01, 00:03"))
        );

        data.withRoute(
                route(pattern("R1", STOP_C, STOP_D))
                        .withTimetable(schedule("00:05, 00:08"))
        );

        data.withTransfer(STOP_B, walk(STOP_C, D30s));

        requestBuilder.searchParams()
                .addAccessPaths(walk(STOP_A, D30s))
                .addEgressPaths(walk(STOP_D, D20s))
                .earliestDepartureTime(T00_00)
                .timetableEnabled(true);

        requestBuilder.profile(RaptorProfile.MULTI_CRITERIA);

        requestBuilder.optimizations().add(Optimization.PARETO_CHECK_AGAINST_DESTINATION);
    }

    @Test
    public void regular() {
        var request = requestBuilder.build();

        var search = new RangeRaptorDynamicSearch<>(config, data, request);

        search.route();

        var destinationHeuristics = search.getDestinationHeuristics();

        assertHeuristics(destinationHeuristics);
    }

    @Test
    public void withConstrainedTransfers() {
        requestBuilder.searchParams().constrainedTransfersEnabled(true);

        var request = requestBuilder.build();

        var search = new RangeRaptorDynamicSearch<>(config, data, request);

        search.route();

        Heuristics destinationHeuristics = search.getDestinationHeuristics();

        assertHeuristics(destinationHeuristics);
    }

    private void assertHeuristics(Heuristics destinationHeuristics) {
        assertNotNull(destinationHeuristics);

        assertArrayLessOrEqual(
                BEST_TRANSFERS,
                destinationHeuristics.bestNumOfTransfersToIntArray(UNREACHED),
                "best number of transfers"
        );
        assertArrayLessOrEqual(
                BEST_TIMES,
                destinationHeuristics.bestTravelDurationToIntArray(UNREACHED),
                "best times"
        );
    }

    private void assertArrayLessOrEqual(int[] expected, int[] actual, String arrayName) {
        assertNotNull(actual);
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertTrue(expected[i] >= actual[i], String.format(
                    "Value %d is greater than %d for index %d in %s",
                    actual[i],
                    expected[i],
                    i,
                    arrayName
            ));
        }
    }
}
