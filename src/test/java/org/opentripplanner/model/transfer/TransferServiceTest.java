package org.opentripplanner.model.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.Stop;

public class TransferServiceTest implements TransferTestData {

    private final TransferService subject = new TransferService();

    @BeforeEach
    public void setup() {
        STOP_A.setParentStation(STATION);
    }

    @Test
    public void findTransfer() {
        // Given:
        var ANY_STOP = Stop.stopForTest("ANY", 67.0, 11.0);
        var A = transfer(STATION_POINT, ROUTE_POINT_11);
        var B = transfer(STOP_POINT_A, STOP_POINT_B);
        var C = transfer(STOP_POINT_A, TRIP_POINT_23);
        var D = transfer(ROUTE_POINT_11, STOP_POINT_B);
        var E = transfer(TRIP_POINT_11, ROUTE_POINT_22);
        var F = transfer(TRIP_POINT_11, TRIP_POINT_23);

        // When: all transfers is added to service
        subject.addAll(List.of(A, B, C, D, E, F));

        // Then:

        // Find the most specific transfer, Trip and stop position match - stops is ignored
        assertEquals(E, subject.findTransfer(STOP_A, STOP_B, TRIP_1, TRIP_2, 1, 2));

        // Find the another specific transfer with the stop position changed
        assertEquals(F, subject.findTransfer(STOP_A, STOP_B, TRIP_1, TRIP_2, 1, 3));

        // Find the specific transfer: TRIP -> STOP when stop position do not match TO point
        assertEquals(D, subject.findTransfer(STOP_A, STOP_B, TRIP_1, TRIP_2, 1, 7));

        // Find the specific transfer: STOP -> TRIP when stop position do not match FROM point
        assertEquals(C, subject.findTransfer(STOP_A, STOP_B, TRIP_1, TRIP_2, 7, 3));

        // Stop position fall back to STOP -> STOP when stop position do not match
        assertEquals(B, subject.findTransfer(STOP_A, STOP_B, TRIP_1, TRIP_2, 7, 7));

        //
        assertEquals(A, subject.findTransfer(STOP_A, ANY_STOP, TRIP_2, TRIP_1, 7, 1));
    }

    @Test
    public void addSameTransferTwiceRetrieveFirstAdded() {
        var A = transfer(STOP_POINT_A, STOP_POINT_B);
        var A_EQ = transfer(STOP_POINT_A, STOP_POINT_B);

        // Adding two transfers between the same stops, will result in only the first being kept
        subject.addAll(List.of(A, A_EQ));

        assertEquals(A, subject.findTransfer(STOP_A, STOP_B, TRIP_1, TRIP_2, 1, 2));
    }

    @Test
    public void listAll() {
        // Given:
        var A = transfer(STATION_POINT, ROUTE_POINT_11);
        var B = transfer(STOP_POINT_A, STOP_POINT_B);
        var C = transfer(STOP_POINT_A, TRIP_POINT_23);

        // When: all transfers is added to service
        subject.addAll(List.of(A, B, C));

        // Then
        assertEquals(List.of(A, B, C), subject.listAll());
    }

    ConstrainedTransfer transfer(TransferPoint from, TransferPoint to) {
        var c = TransferConstraint.create().build();
        return new ConstrainedTransfer(null, from, to, c);
    }
}