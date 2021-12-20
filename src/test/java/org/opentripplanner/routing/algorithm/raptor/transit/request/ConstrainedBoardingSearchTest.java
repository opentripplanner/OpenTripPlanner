package org.opentripplanner.routing.algorithm.raptor.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.transfer.TransferConstraint.REGULAR_TRANSFER;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.StopTransferPoint;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.model.transfer.TripTransferPoint;

public class ConstrainedBoardingSearchTest implements TestTransitCaseData {

    private static final boolean FORWARD = true;
    private static final boolean REVERSE = false;
    private static final int TRIP_1_INDEX = 0;
    private static final int TRIP_2_INDEX = 1;


    private TestRouteData route1;
    private TestRouteData route2;

    /**
     * Create transit data with 2 routes with a trip each.
     * <pre>
     *                              STOPS
     *                     A      B      C      D
     * Route R1
     *   - Trip R1-1:    10:00  10:10  10:20
     *   - Trip R1-2:    10:05  10:15  10:25
     * Route R2
     *   - Trip R2-1:           10:15  10:30  10:40
     *   - Trip R2-2:           10:20  10:35  10:45
     * </pre>
     * <ul>
     *     <li>
     *         The transfer at stop B is tight between trip R1-2 and R2-1. There is no time between
     *         the arrival and departure, and it is only possible to transfer if the transfer is
     *         stay-seated or guaranteed. For other types of constrained transfers we should board
     *         the next trip 'R2-2'.
     *     </li>
     *     <li>
     *         The transfer at stop C is allow regular transfers between trip R1-2 and R2-1.
     *     </li>
     *     <li>
     *         R1-1 is the fallback int the reverse search in the same way as R2-2 is the fallback
     *         int the forward search.
     *     </li>
     * </ul>
     * The
     *
     */
    @BeforeEach
    void setup() {
        route1 = new TestRouteData(
                "R1", TransitMode.RAIL,
                List.of(STOP_A, STOP_B, STOP_C),
                "10:00 10:10 10:20",
                "10:05 10:15 10:25"
        );

        route2 = new TestRouteData(
                "R2", TransitMode.BUS,
                List.of(STOP_B, STOP_C, STOP_D),
                "10:15 10:30 10:40",
                "10:20 10:35 10:45"
        );
    }

    @Test
    void transferExist() {
        var transfers = new TIntObjectHashMap<List<ConstrainedTransfer>>();
        int targetStopPos = 3;
        transfers.put(targetStopPos, List.of());

        // Forward
        var subject = new ConstrainedBoardingSearch(true, transfers);
        assertTrue(subject.transferExist(targetStopPos));

        // Reverse
        subject = new ConstrainedBoardingSearch(false, transfers);
        assertTrue(subject.transferExist(targetStopPos));
    }

    @Test
    void findGuaranteedTransferWithZeroConnectionTime() {
        var transfers = new TIntObjectHashMap<List<ConstrainedTransfer>>();
        int sourceStopPos = route1.stopPosition(STOP_B);
        int targetStopPos = route2.stopPosition(STOP_B);

        TransferConstraint constraint = TransferConstraint.create().guaranteed().build();
        ConstrainedTransfer txGuaranteed = new ConstrainedTransfer(
                id("Guaranteed"),
                new TripTransferPoint(route1.lastTrip().trip(), sourceStopPos),
                new TripTransferPoint(route2.firstTrip().trip(), targetStopPos),
                constraint
        );
        List<ConstrainedTransfer> constrainedTransfers = List.of(txGuaranteed);
        transfers.put(targetStopPos, constrainedTransfers);

        testTransferSearch(STOP_B, constrainedTransfers, TRIP_1_INDEX, constraint);
    }

    @Test
    void findNextTransferWhenFirstTripIsNoAllowed() {
        var transfers = new TIntObjectHashMap<List<ConstrainedTransfer>>();
        int sourceStopPos = route1.stopPosition(STOP_C);
        int targetStopPos = route2.stopPosition(STOP_C);

        TransferConstraint constraint = TransferConstraint.create().notAllowed().build();
        ConstrainedTransfer txGuaranteed = new ConstrainedTransfer(
                id("NOT-ALLOWED"),
                new TripTransferPoint(route1.lastTrip().trip(), sourceStopPos),
                new TripTransferPoint(route2.firstTrip().trip(), targetStopPos),
                constraint
        );
        List<ConstrainedTransfer> constrainedTransfers = List.of(txGuaranteed);
        transfers.put(targetStopPos, constrainedTransfers);

        testTransferSearch(STOP_C, constrainedTransfers, TRIP_2_INDEX, REGULAR_TRANSFER);
    }

    @Test
    @Disabled("This test fail, so we will try to fix the problem in the nex commit")
    void blockTransferWhenNoAllowedMatchesAllTripsInRoute() {
        var transfers = new TIntObjectHashMap<List<ConstrainedTransfer>>();
        int targetStopPos = route2.stopPosition(STOP_C);

        TransferConstraint constraint = TransferConstraint.create().notAllowed().build();
        ConstrainedTransfer txGuaranteed = new ConstrainedTransfer(
                id("NOT-ALLOWED"),
                new StopTransferPoint(STOP_C),
                new StopTransferPoint(STOP_C),
                constraint
        );
        List<ConstrainedTransfer> constrainedTransfers = List.of(txGuaranteed);
        transfers.put(targetStopPos, constrainedTransfers);

        testTransferSearch(STOP_C, constrainedTransfers, TRIP_1_INDEX, constraint);
    }

    void testTransferSearch(
            Stop transferStop,
            List<ConstrainedTransfer> constraints,
            int expTripIndex,
            TransferConstraint expConstraint
    ) {
        testTransferSearchForward(transferStop, constraints, expTripIndex, expConstraint);
        // Swap expected trip index for reverse search
        int revExpTripIndex = expTripIndex == TRIP_1_INDEX ? TRIP_2_INDEX : TRIP_1_INDEX;
        testTransferSearchReverse(transferStop, constraints, revExpTripIndex, expConstraint);
    }

    void testTransferSearchForward(
            Stop transferStop,
            List<ConstrainedTransfer> constraints,
            int expectedTripIndex,
            TransferConstraint expectedConstraint
    ) {
        int targetStopPos = route2.stopPosition(transferStop);
        var transfers = new TIntObjectHashMap<List<ConstrainedTransfer>>();
        int stopIndex = stopIndex(transferStop);
        int sourceArrivalTime = route1.lastTrip().getStopTime(transferStop).getArrivalTime();

        transfers.put(targetStopPos, constraints);

        // Forward
        var subject = new ConstrainedBoardingSearch(FORWARD, transfers);

        // Check that transfer exist
        assertTrue(subject.transferExist(targetStopPos));

        var boarding = subject.find(
                route2.getTimetable(),
                route1.lastTrip().getTripSchedule(),
                stopIndex,
                sourceArrivalTime
        );

        assertNotNull(boarding);
        assertEquals(expectedConstraint, boarding.getTransferConstraint());
        assertEquals(stopIndex , boarding.getBoardStopIndex());
        assertEquals(targetStopPos, boarding.getStopPositionInPattern());
        assertEquals(expectedTripIndex, boarding.getTripIndex());
    }


    void testTransferSearchReverse(
            Stop transferStop,
            List<ConstrainedTransfer> constraints,
            int expectedTripIndex,
            TransferConstraint expectedConstraint
    ) {
        int targetStopPos = route1.stopPosition(transferStop);
        var transfers = new TIntObjectHashMap<List<ConstrainedTransfer>>();
        int stopIndex = stopIndex(transferStop);
        int sourceArrivalTime = route2.firstTrip().getStopTime(transferStop).getDepartureTime();

        transfers.put(targetStopPos, constraints);

        // Forward
        var subject = new ConstrainedBoardingSearch(REVERSE, transfers);

        // Check that transfer exist
        assertTrue(subject.transferExist(targetStopPos));

        var boarding = subject.find(
                route1.getTimetable(),
                route2.firstTrip().getTripSchedule(),
                stopIndex,
                sourceArrivalTime
        );

        assertNotNull(boarding);
        assertEquals(expectedConstraint, boarding.getTransferConstraint());
        assertEquals(stopIndex , boarding.getBoardStopIndex());
        assertEquals(targetStopPos, boarding.getStopPositionInPattern());
        assertEquals(expectedTripIndex, boarding.getTripIndex());
    }
}
