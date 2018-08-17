package org.opentripplanner.routing.core;

import junit.framework.TestCase;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;

public class TestTransferTable extends TestCase {

    /**
     * Test transfer table
     */
    public void testTransferTable() {
        // Setup from stop
        Stop fromStop = new Stop();
        fromStop.setId(new FeedScopedId("A1", "S1"));
        
        // Setup to stop
        Stop toStop = new Stop();
        toStop.setId(new FeedScopedId("A1", "S2"));
        
        // Setup to stop parent
        Stop toStopParent = new Stop();
        toStopParent.setId(new FeedScopedId("A1", "S3"));
        toStop.setParentStation("S3");
        
        // Setup from trip with route
        Route fromRoute = new Route();
        fromRoute.setId(new FeedScopedId("A1", "R1"));
        Trip fromTrip = new Trip();
        fromTrip.setId(new FeedScopedId("A1", "T1"));
        fromTrip.setRoute(fromRoute);
        
        // Setup to trip with route
        Route toRoute = new Route();
        toRoute.setId(new FeedScopedId("A1", "R2"));
        Trip toTrip = new Trip();
        toTrip.setId(new FeedScopedId("A1", "T2"));
        toTrip.setRoute(toRoute);
        
        // Setup second to trip with route
        Route toRoute2 = new Route();
        toRoute2.setId(new FeedScopedId("A1", "R3"));
        Trip toTrip2 = new Trip();
        toTrip2.setId(new FeedScopedId("A1", "T3"));
        toTrip2.setRoute(toRoute2);
        
        // Create transfer table
        TransferTable table = new TransferTable();
        
        // Check transfer times
        assertEquals(StopTransfer.UNKNOWN_TRANSFER, table.getTransferTime(fromStop, toStop, fromTrip, toTrip, true));
        assertEquals(StopTransfer.UNKNOWN_TRANSFER, table.getTransferTime(fromStop, toStop, fromTrip, toTrip2, true));
        assertEquals(StopTransfer.UNKNOWN_TRANSFER, table.getTransferTime(fromStop, toStopParent, fromTrip, toTrip, true));
        assertEquals(StopTransfer.UNKNOWN_TRANSFER, table.getTransferTime(fromStop, toStopParent, fromTrip, toTrip2, true));
        
        // Add transfer to parent stop, specificity 0
        table.addTransferTime(fromStop, toStopParent, null, null, null, null, StopTransfer.PREFERRED_TRANSFER);
        assertEquals(StopTransfer.PREFERRED_TRANSFER, table.getTransferTime(fromStop, toStop, fromTrip, toTrip, true));
        assertEquals(StopTransfer.PREFERRED_TRANSFER, table.getTransferTime(fromStop, toStop, fromTrip, toTrip2, true));
        assertEquals(StopTransfer.PREFERRED_TRANSFER, table.getTransferTime(fromStop, toStopParent, fromTrip, toTrip, true));
        assertEquals(StopTransfer.PREFERRED_TRANSFER, table.getTransferTime(fromStop, toStopParent, fromTrip, toTrip2, true));
        
        // Check going forward and backwards in time
        assertEquals(StopTransfer.PREFERRED_TRANSFER, table.getTransferTime(fromStop, toStop, fromTrip, toTrip, true));
        assertEquals(StopTransfer.UNKNOWN_TRANSFER, table.getTransferTime(fromStop, toStop, fromTrip, toTrip, false));
        assertEquals(StopTransfer.UNKNOWN_TRANSFER, table.getTransferTime(toStop, fromStop, toTrip, fromTrip, true));
        assertEquals(StopTransfer.PREFERRED_TRANSFER, table.getTransferTime(toStop, fromStop, toTrip, fromTrip, false));
        
        // Add transfer to child stop, specificity 1
        table.addTransferTime(fromStop, toStop, null, toRoute, null, null, StopTransfer.FORBIDDEN_TRANSFER);
        assertEquals(StopTransfer.FORBIDDEN_TRANSFER, table.getTransferTime(fromStop, toStop, fromTrip, toTrip, true));
        assertEquals(StopTransfer.PREFERRED_TRANSFER, table.getTransferTime(fromStop, toStop, fromTrip, toTrip2, true));
        assertEquals(StopTransfer.PREFERRED_TRANSFER, table.getTransferTime(fromStop, toStopParent, fromTrip, toTrip, true));
        assertEquals(StopTransfer.PREFERRED_TRANSFER, table.getTransferTime(fromStop, toStopParent, fromTrip, toTrip2, true));
        
        // Add transfer to parent stop, specificity 1
        table.addTransferTime(fromStop, toStopParent, null, toRoute2, null, null, StopTransfer.TIMED_TRANSFER);
        assertEquals(StopTransfer.FORBIDDEN_TRANSFER, table.getTransferTime(fromStop, toStop, fromTrip, toTrip, true));
        assertEquals(StopTransfer.TIMED_TRANSFER, table.getTransferTime(fromStop, toStop, fromTrip, toTrip2, true));
        assertEquals(StopTransfer.PREFERRED_TRANSFER, table.getTransferTime(fromStop, toStopParent, fromTrip, toTrip, true));
        assertEquals(StopTransfer.TIMED_TRANSFER, table.getTransferTime(fromStop, toStopParent, fromTrip, toTrip2, true));
        
        // Add transfer to child stop, specificity 2
        table.addTransferTime(fromStop, toStop, null, toRoute2, null, toTrip2, 4);
        assertEquals(StopTransfer.FORBIDDEN_TRANSFER, table.getTransferTime(fromStop, toStop, fromTrip, toTrip, true));
        assertEquals(4, table.getTransferTime(fromStop, toStop, fromTrip, toTrip2, true));
        assertEquals(StopTransfer.PREFERRED_TRANSFER, table.getTransferTime(fromStop, toStopParent, fromTrip, toTrip, true));
        assertEquals(StopTransfer.TIMED_TRANSFER, table.getTransferTime(fromStop, toStopParent, fromTrip, toTrip2, true));
        
        // Add transfer to parent stop and unknown to child stop, specificity 3
        // WARNING: don't add transfers with StopTransfer.UNKNOWN_TRANSFER in non-testing code
        table.addTransferTime(fromStop, toStop, fromRoute, null, null, toTrip, StopTransfer.UNKNOWN_TRANSFER);
        table.addTransferTime(fromStop, toStopParent, fromRoute, null, null, toTrip, 5);
        assertEquals(5, table.getTransferTime(fromStop, toStop, fromTrip, toTrip, true));
        assertEquals(4, table.getTransferTime(fromStop, toStop, fromTrip, toTrip2, true));
        assertEquals(5, table.getTransferTime(fromStop, toStopParent, fromTrip, toTrip, true));
        assertEquals(StopTransfer.TIMED_TRANSFER, table.getTransferTime(fromStop, toStopParent, fromTrip, toTrip2, true));
        
        // Add transfer to child stop, specificity 4
        table.addTransferTime(fromStop, toStop, null, null, fromTrip, toTrip2, 6);
        assertEquals(5, table.getTransferTime(fromStop, toStop, fromTrip, toTrip, true));
        assertEquals(6, table.getTransferTime(fromStop, toStop, fromTrip, toTrip2, true));
        assertEquals(5, table.getTransferTime(fromStop, toStopParent, fromTrip, toTrip, true));
        assertEquals(StopTransfer.TIMED_TRANSFER, table.getTransferTime(fromStop, toStopParent, fromTrip, toTrip2, true));
    }
}
