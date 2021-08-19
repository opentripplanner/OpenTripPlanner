package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;
import static org.opentripplanner.model.transfer.TransferPriority.NOT_ALLOWED;
import static org.opentripplanner.model.transfer.TransferPriority.PREFERRED;
import static org.opentripplanner.model.transfer.TransferPriority.RECOMMENDED;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TestTransferBuilder.txConstrained;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferGeneratorDummy.dummyTransferGenerator;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferGeneratorDummy.tx;
import static org.opentripplanner.util.time.TimeUtils.time;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;

/**
 * <pre>
 * POSSIBLE TRANSFERS
 * Transfers        B-C 1m     C-D 2m       D-E 3m     E-F 4m      F-G 5m
 * Constraint       ALLOWED  RECOMMENDED  PREFERRED  GUARANTIED  STAY_SEATED
 * Trip 1    10:02  B 10:10    C 10:15     D 10:20     E 10:25     F 10:30
 * Trip 2           C 10:13    D 10:18     E 10:24     G 10:30     G 10:36    10:40
 * </pre>
 *
 * Case: There is 5 possible places to transfer in this setup. We want to test that the correct one
 *     is picked according to the constraint. We can test all relevant cases by changing the egress
 *     stop, since the transfers are ordered with the highest priority last. Transfer in the same stop
 *     is NOT_ALLOWED.
 *
 * Expect: The highest priority should be picked
 *
 * Module under test: We are testing the Optimized Transfer Service, not Routing it self. So, the 
 *     path will always include two trips with one transfer selected even where single trip might 
 *     be found by the router. 
 *
 * Note! This test uses some of the constants and utility methods of {@link
 *     OptimizePathServiceTest}
 */
@SuppressWarnings("SameParameterValue")
public class OptimizePathServiceConstrainedTest implements RaptorTestConstants {

    /**
     * The exact start time to walk to stop A to catch Trip_1 with 40s board slack
     */
    private final int START_TIME_T1 = time("10:00:20");


    // Given
    TestTripSchedule trip1 = TestTripSchedule.schedule()
            .pattern("T1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E, STOP_F)
            .times("10:02 10:10 10:15 10:20 10:25 10:30").build();

    TestTripSchedule trip2 = TestTripSchedule.schedule()
            .pattern("T2", STOP_C, STOP_D, STOP_E, STOP_F, STOP_G, STOP_H)
            .times("10:13 10:18 10:24 10:30 10:36 10:40").build();

    TransferGenerator<TestTripSchedule> transfers = dummyTransferGenerator(
            List.of(
                    tx(txConstrained(trip1, STOP_B, trip2, STOP_C).priority(ALLOWED), D1m),
                    tx(txConstrained(trip1, STOP_C, trip2, STOP_D).priority(RECOMMENDED), D2m),
                    tx(txConstrained(trip1, STOP_D, trip2, STOP_E).priority(PREFERRED), D3m),
                    tx(txConstrained(trip1, STOP_E, trip2, STOP_F).guaranteed(), D4m),
                    tx(txConstrained(trip1, STOP_F, trip2, STOP_G).staySeated(), D5m),
                    tx(txConstrained(trip1, STOP_C, trip2, STOP_C).priority(NOT_ALLOWED)),
                    tx(txConstrained(trip1, STOP_D, trip2, STOP_D).priority(NOT_ALLOWED)),
                    tx(txConstrained(trip1, STOP_E, trip2, STOP_E).priority(NOT_ALLOWED)),
                    tx(txConstrained(trip1, STOP_F, trip2, STOP_F).priority(NOT_ALLOWED))
            )
    );

    @Test
    public void testTransferPriorityAllowed() {
        testPriority(
                STOP_D, ALLOWED,
                "1 ~ BUS T1 10:02 10:10 ~ 2 ~ Walk 1m ~ 3 ~ BUS T2 10:13 10:18 ~ 4 [10:00:20 10:18:20 18m $1180]"
        );
    }

    @Test
    public void testTransferPriorityRecommended() {
        testPriority(
                STOP_E, RECOMMENDED,
                "1 ~ BUS T1 10:02 10:15 ~ 3 ~ Walk 2m ~ 4 ~ BUS T2 10:18 10:24 ~ 5 [10:00:20 10:24:20 24m $1600]"
        );
    }

    @Test
    public void testTransferPriorityPreferred() {
        testPriority(
                STOP_F, PREFERRED,
                "1 ~ BUS T1 10:02 10:20 ~ 4 ~ Walk 3m ~ 5 ~ BUS T2 10:24 10:30 ~ 6 [10:00:20 10:30:20 30m $2020]"
        );
    }

    @Test
    public void testTransferGuaranteed() {
        testGuaranteed(
                STOP_G,
                "1 ~ BUS T1 10:02 10:25 ~ 5 ~ Walk 4m ~ 6 ~ BUS T2 10:30 10:36 ~ 7 [10:00:20 10:36:20 36m $2440]"
        );
    }
    @Test
    public void testTransferStaySeated() {
        testStaySeated(
                STOP_H,
                "1 ~ BUS T1 10:02 10:30 ~ 6 ~ Walk 5m ~ 7 ~ BUS T2 10:36 10:40 ~ 8 [10:00:20 10:40:20 40m $2740]"
        );
    }


    /* private methods */

    private void testStaySeated(int egressStop, String expItinerary) {
        doTest(egressStop, true, false, ALLOWED, expItinerary);
    }

    private void testGuaranteed(int egressStop, String expItinerary) {
        doTest(egressStop, false, true, ALLOWED, expItinerary);
    }

    private void testPriority(int egressStop, TransferPriority expPriority, String expItinerary) {
        doTest(egressStop, false, false, expPriority, expItinerary);
    }

    private void doTest(
            int egressStop,
            boolean expStaySeated,
            boolean expGuaranteed,
            TransferPriority expPriority,
            String expItinerary
    ) {
        var original = OptimizePathServiceTest.pathBuilder()
                .access(START_TIME_T1, 0, STOP_A)
                .bus(trip1, STOP_B)
                .walk(D1m, STOP_C)
                .bus(trip2, egressStop)
                .egress(D0s);

        var subject = OptimizePathServiceTest.subject(transfers);

        // Find the path with the lowest cost
        var result = subject.findBestTransitPath(original);

        assertEquals(1, result.size(), result.toString());

        var it = result.iterator().next();

        assertEquals(expItinerary, it.toString());

        // Verify the attached Transfer is exist and is valid
        var transfer = it.getTransferTo(it.accessLeg().nextLeg().nextTransitLeg());

        if (expPriority != null) {
            assertNotNull(transfer);
        }
        if (transfer != null) {
            assertEquals(expPriority, transfer.getPriority(), transfer.toString());
            assertEquals(expStaySeated, transfer.isStaySeated(), transfer.toString());
            assertEquals(expGuaranteed, transfer.isGuaranteed(), transfer.toString());
        }
    }
}
