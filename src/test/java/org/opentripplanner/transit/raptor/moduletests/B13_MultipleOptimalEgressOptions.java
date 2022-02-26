package org.opentripplanner.transit.raptor.moduletests;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.flex;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.STANDARD;
import static org.opentripplanner.transit.raptor.api.request.SearchDirection.REVERSE;
import static org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider.defaultSlackProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptor.transit.cost.RaptorCostConverter;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;

/**
 * FEATURE UNDER TEST
 * <p>
 * This test focus on egress on-foot and flex egress. You are not allowed to have two walk legs
 * after each other, so depending on how you arrived at the stop where the egress start, the
 * walking option might not be possible.
 * <p>
 * Test case:
 * <img src="images/B13.svg" width="432" height="212" />
 * <p>
 * <pre>
 * // Allowed paths
 * A ~ L1 ~ C ~ Walk ~ D
 * A ~ L1 ~ C ~ Flex ~ D
 * A ~ L2 ~ B ~ Walk 2m ~ C ~ Flex ~ D
 * // Not allowed
 * A ~ L2 ~ B ~ Walk 2m ~ C ~ Walk ~ D
 * </pre>
 * To alternate witch egress leg is the best, we change the egress walk between 5 minutes(walking is better than the path with flex) and 7 minutes(the path with flex egress become the fastest option). Note! There is 1 minute transfer slack.
 */
public class B13_MultipleOptimalEgressOptions implements RaptorTestConstants {

    private static final String EXPECTED_PATH_FLEX =
            "A ~ BUS R2 0:05 0:15 ~ B ~ Walk 2m ~ C ~ Flex 8m 1x [0:05 0:26 21m";
    private static final String EXPECTED_STD_FLEX = EXPECTED_PATH_FLEX +"]";
    private static final String EXPECTED_MC_FLEX = EXPECTED_PATH_FLEX +" $2100]";

    private static final String EXPECTED_PATH_WALK =
            "A ~ BUS R1 0:05 0:20 ~ C ~ Walk 3m [0:05 0:23 18m ";
    private static final String EXPECTED_STD_WALK = EXPECTED_PATH_WALK +"]";
    private static final String EXPECTED_MC_WALK = EXPECTED_PATH_WALK +" $1860]";


    private static final int COST_10m = RaptorCostConverter.toRaptorCost(D10m);

    private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
            RaptorConfig.defaultConfigForTest()
    );
    private final TestTransitData data = new TestTransitData();
    private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();

    @BeforeEach
    public void setup() {
        data.withRoutes(
                route("R1", STOP_A, STOP_C).withTimetable(schedule("0:05 0:20")),
                route("R2", STOP_A, STOP_B).withTimetable(schedule("0:05 0:15"))
        );
        requestBuilder.searchParams()
                .earliestDepartureTime(T00_00)
                .searchWindowInSeconds(D20m)
                .latestArrivalTime(T00_30);

        // We will test board- and alight-slack in a separate test
        requestBuilder.slackProvider(defaultSlackProvider(D1m, D0s, D0s));

        requestBuilder.searchParams().addAccessPaths(walk(STOP_A, D0s));

        data.withTransfer(STOP_B, walk(STOP_C, D2m));

        // Set ModuleTestDebugLogging.DEBUG=true to enable debugging output
        ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
    }

    private void withFlexAsOptimalEgress() {
        requestBuilder.searchParams()
                .addEgressPaths(
                        flex(STOP_C, D8m, 1, COST_10m),
                        walk(STOP_C, D10m)
                );

    }

    private void withWalkingAsOptimalEgress() {
        requestBuilder.searchParams().addEgressPaths(
                flex(STOP_C, D8m, 1, COST_10m),
                walk(STOP_C, D3m)
        );
    }

    @Test
    public void standardFlex() {
        withFlexAsOptimalEgress();
        requestBuilder.profile(STANDARD);
        assertEquals(EXPECTED_STD_FLEX, runSearch());
    }

    @Test
    public void standardWalking() {
        withWalkingAsOptimalEgress();
        requestBuilder.profile(STANDARD);
        assertEquals(EXPECTED_STD_WALK, runSearch());
    }

    @Test
    public void standardReverseFlex() {
        withFlexAsOptimalEgress();
        requestBuilder.profile(STANDARD).searchDirection(REVERSE);
        assertEquals(EXPECTED_STD_FLEX, runSearch());
    }

    @Test
    public void standardReverseWalking() {
        withWalkingAsOptimalEgress();
        requestBuilder.profile(STANDARD).searchDirection(REVERSE);
        assertEquals(EXPECTED_STD_WALK, runSearch());
    }

    @Test
    public void multiCriteriaFlex() {
        withFlexAsOptimalEgress();
        requestBuilder.profile(MULTI_CRITERIA);
        assertEquals(EXPECTED_MC_FLEX, runSearch());
    }

    @Test
    public void multiCriteriaWalking() {
        withWalkingAsOptimalEgress();
        requestBuilder.profile(MULTI_CRITERIA);
        assertEquals(EXPECTED_MC_WALK, runSearch());
    }

    private String runSearch() {
        return pathsToString(raptorService.route(requestBuilder.build(), data));
    }
}