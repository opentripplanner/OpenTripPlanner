package org.opentripplanner.model.transfer;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.transfer.Transfer.MAX_WAIT_TIME_NOT_SET;
import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;

import java.util.List;
import org.junit.Test;

public class TransferServiceTest implements TransferTestData {

    private final TransferService subject = new TransferService();


    @Test
    public void addOneTransferForEachCombinationOfFromToTypesAndRetriveEachOfThem() {
        // Given:
        var A = transfer(STOP_POINT_A, STOP_POINT_B);
        var B = transfer(STOP_POINT_A, TRIP_POINT_23);
        var C = transfer(ROUTE_POINT_11, STOP_POINT_B);
        var D = transfer(TRIP_POINT_11, ROUTE_POINT_22);
        var E = transfer(TRIP_POINT_11, TRIP_POINT_23);

        // When: all transfers is added to service
        subject.addAll(List.of(A, B, C, D, E));

        /* THEN */

        // Find the most specific transfer, Trip and stop position match - stops is ignored
        assertEquals(D, subject.findTransfer(STOP_A, STOP_B, TRIP_1, TRIP_2, 1, 2));

        // Find the another specific transfer with the stop position changed
        assertEquals(E, subject.findTransfer(STOP_A, STOP_B, TRIP_1, TRIP_2, 1, 3));

        // Find the specific transfer: TRIP -> STOP when stop position do not match TO point
        assertEquals(C, subject.findTransfer(STOP_A, STOP_B, TRIP_1, TRIP_2, 1, 7));

        // Find the specific transfer: STOP -> TRIP when stop position do not match FROM point
        assertEquals(B, subject.findTransfer(STOP_A, STOP_B, TRIP_1, TRIP_2, 7, 3));

        // Stop position fall back to STOP -> STOP when stop position do not match
        assertEquals(A, subject.findTransfer(STOP_A, STOP_B, TRIP_1, TRIP_2, 7, 7));
    }

    @Test
    public void addSameTransferTwiceRetrieveFirstAdded() {
        var A = transfer(STOP_POINT_A, STOP_POINT_B);
        var A_EQ = transfer(STOP_POINT_A, STOP_POINT_B);

        // Adding two transfers between the same stops, will result in only the first being kept
        subject.addAll(List.of(A, A_EQ));

        assertEquals(A, subject.findTransfer(STOP_A, STOP_B, TRIP_1, TRIP_2, 1, 2));
    }


    Transfer transfer(TransferPoint from, TransferPoint to) {
        return new Transfer(from, to, ALLOWED, false, false, MAX_WAIT_TIME_NOT_SET);
    }
}