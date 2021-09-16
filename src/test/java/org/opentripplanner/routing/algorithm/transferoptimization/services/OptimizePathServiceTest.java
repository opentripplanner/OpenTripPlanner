package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TestTransferBuilder.txConstrained;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferGeneratorDummy.dummyTransferGenerator;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferGeneratorDummy.tx;
import static org.opentripplanner.util.time.TimeUtils.time;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.api.PathBuilder;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.DefaultCostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;


public class OptimizePathServiceTest implements RaptorTestConstants {

    /**
     * The exact start time to walk to stop A to catch Trip_1 with 40s board slack
     */
    private static final int START_TIME_T1 = time("10:00:20");
    private static final int TRANSFER_SLACK = D1m;
    private static final int BOARD_SLACK = D40s;
    private static final int ALIGHT_SLACK = D20s;
    private static final int BOARD_COST_SEC = 10;
    private static final int TRANSFER_COST_SEC = 20;
    private static final double WAIT_RELUCTANCE = 1.0;

    private static final RaptorSlackProvider SLACK_PROVIDER = RaptorSlackProvider
            .defaultSlackProvider(TRANSFER_SLACK, BOARD_SLACK, ALIGHT_SLACK);

    public static final CostCalculator<TestTripSchedule> COST_CALCULATOR = new DefaultCostCalculator<>(
            BOARD_COST_SEC,
            TRANSFER_COST_SEC,
            WAIT_RELUCTANCE,
            null,
            null
    );

    static PathBuilder pathBuilder() {
        return new PathBuilder(ALIGHT_SLACK, COST_CALCULATOR);
    }

    /**
     * A Path without any transfers should be returned without any change.
     */
    @Test
    public void testTripWithoutTransfers() {
        // Given a trip A-B-C-D
        var trip1 = TestTripSchedule.schedule()
                .pattern("T1", STOP_A, STOP_B, STOP_C, STOP_D)
                .times("10:02 10:10 10:20 10:30").build();

        // Use only in-same-stop transfers
        var transfers = dummyTransferGenerator();

        // and a path: Walk ~ B ~ T1 ~ C ~ Walk
        var original = pathBuilder()
                .access(START_TIME_T1, D1m, STOP_B)
                .bus(trip1, STOP_C)
                .egress(D1m);

        var subject = subject(transfers);

        // When
        var result = subject.findBestTransitPath(original);

        // Then expect a set containing the original path
        assertEquals(original.toStringDetailed(), first(result).toStringDetailed());
        assertEquals(1, result.size());
    }

    /**
     * This test emulates the normal case were there is only one option to transfer between two
     * trips and we should find the exact same option. The path should exactly match the original
     * path after the path is reconstructed.
     */
    @Test
    public void testTripWithOneTransfer() {
        // Given
        var trip1 = TestTripSchedule.schedule()
                .arrDepOffset(D0s)
                .pattern("T1", STOP_A, STOP_B, STOP_C, STOP_D)
                .times("10:02 10:10 10:20 10:30").build();

        var trip2 = TestTripSchedule.schedule()
                .arrDepOffset(D0s)
                .pattern("T2", STOP_E, STOP_F, STOP_G)
                .times( "10:12 10:22 10:50").build();

        var transfers = dummyTransferGenerator(
                List.of(tx(trip1, STOP_C, D30s, STOP_F, trip2))
        );

        // Path:  Access ~ B ~ T1 ~ C ~ Walk 30s ~ D ~ T2 ~ E ~ Egress
        var original = pathBuilder()
                .access(START_TIME_T1, D1m, STOP_B)
                .bus(trip1, STOP_C)
                .walk(D30s, STOP_F)
                .bus(trip2, STOP_G)
                .egress(D1m);

        var subject = subject(transfers);

        // When
        var result = subject.findBestTransitPath(original);

        assertEquals(original.toStringDetailed(), first(result).toStringDetailed());
        assertEquals(1, result.size());
    }

    /**
     * DEPARTURE TIMES
     * Stop        A      B      C      D      E      F      G
     * Trip 1    10:02  10:10         10:20
     * Trip 2           10:12  10:15  10:22         10:35
     * Trip 3                                10:24  10:37  10:49
     */
    @Test
    public void testPathWithThreeTripsAndMultiplePlacesToTransfer() {
        // Given
        var trip1 = TestTripSchedule.schedule()
                .pattern("T1", STOP_A, STOP_B, STOP_D)
                .times("10:02 10:10 10:20").build();

        var trip2 = TestTripSchedule.schedule()
                .pattern("T2", STOP_B, STOP_C, STOP_D, STOP_F)
                .times("10:12 10:15 10:22 10:35").build();

        var trip3 = TestTripSchedule.schedule()
                .pattern("T3", STOP_E, STOP_F, STOP_G)
                .times("10:24 10:37 10:49").build();

        var transfers = dummyTransferGenerator(
                List.of(
                        tx(trip1, STOP_B, trip2),
                        tx(trip1, STOP_B, D30s, STOP_C, trip2),
                        tx(trip1, STOP_D, trip2)
                ),
                List.of(
                        tx(trip2, STOP_D, D30s, STOP_E, trip3),
                        tx(trip2, STOP_F, trip3)
                )
        );

        var original = pathBuilder()
                .access(START_TIME_T1, D0s, STOP_A)
                .bus(trip1, STOP_B)
                .bus(trip2, STOP_D)
                .walk(D30s, STOP_E)
                .bus(trip3, STOP_G)
                .egress(D0s);

        var subject = subject(transfers);

        // Find the path with the lowest cost
        var result = subject.findBestTransitPath(original);

        assertEquals(
                "[1 ~ BUS T1 10:02 10:10 ~ 2 ~ BUS T2 10:12 10:35 ~ 6 ~ "
                        + "BUS T3 10:37 10:49 ~ 7 [10:00:20 10:49:20 49m $3010]]",
                result.toString()
        );
    }

    /**
     * DEPARTURE TIMES
     * Stop        A      B      C      D
     * Trip 1    10:02  10:10  10:15
     * Trip 2           10:13  10:17  10:30
     *
     * Case: Transfer at stop B is returned, but transfer at stop C i guaranteed
     * Expect: Transfer at C and transfer info attached
     */
    @Test
    public void testGuaranteedTransferIsPreferred() {
        // Given
        var trip1 = TestTripSchedule.schedule()
                .pattern("T1", STOP_A, STOP_B, STOP_C)
                .times("10:02 10:10 10:15").build();

        var trip2 = TestTripSchedule.schedule()
                .pattern("T2", STOP_B, STOP_C, STOP_D)
                .times("10:13 10:17 10:30").build();

        var transfers = dummyTransferGenerator(
                List.of(
                        tx(trip1, STOP_B, trip2),
                        tx(txConstrained(trip1, STOP_C, trip2, STOP_C).guaranteed())
                )
        );

        var original = pathBuilder()
                .access(START_TIME_T1, 0, STOP_A)
                .bus(trip1, STOP_B)
                .bus(trip2, STOP_D)
                .egress(D0s);

        var subject = subject(transfers);

        // Find the path with the lowest cost
        var result = subject.findBestTransitPath(original);

        assertEquals(1, result.size(), result.toString());

        var it = result.iterator().next();

        assertEquals(
                "1 ~ BUS T1 10:02 10:15 ~ 3 ~ BUS T2 10:17 10:30 ~ 4 [10:00:20 10:30:20 30m $1840]",
                it.toString()
        );
        // Verify the attached Transfer is exist and is valid
        assertEquals(
                "Transfer{from: (trip: BUS T1:10:02, stopPos: 2), to: (trip: BUS T2:10:13, stopPos: 1), guaranteed}",
                it.getTransferTo(it.accessLeg().nextLeg().nextLeg()).toString()
        );
    }


    /* private methods */

    static OptimizePathService<TestTripSchedule> subject(
            TransferGenerator<TestTripSchedule> generator
    ) {
        return new OptimizePathService<>(
                generator,
                COST_CALCULATOR,
                SLACK_PROVIDER,
                PathLeg::generalizedCostTotal,
                TransferOptimizedFilterFactory.filter(true, true)
        );
    }

    static <T> T first(Collection<T> c) {
        return c.stream().findFirst().orElseThrow();
    }
}