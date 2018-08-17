package org.opentripplanner.routing.core;

import junit.framework.TestCase;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

public class TestStopTransfer extends TestCase {

    /**
     * Test different stop transfers
     */
    public void testStopTransfer() {
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
        
        // Create StopTransfer
        StopTransfer transfer = new StopTransfer();
        assertEquals(StopTransfer.UNKNOWN_TRANSFER, transfer.getTransferTime(fromTrip, toTrip));
        assertEquals(StopTransfer.UNKNOWN_TRANSFER, transfer.getTransferTime(fromTrip, toTrip2));
        
        // Add empty SpecificTransfer, specificity 0
        transfer.addSpecificTransfer(new SpecificTransfer((FeedScopedId) null, null, null, null, StopTransfer.FORBIDDEN_TRANSFER));
        assertEquals(StopTransfer.FORBIDDEN_TRANSFER, transfer.getTransferTime(fromTrip, toTrip));
        assertEquals(StopTransfer.FORBIDDEN_TRANSFER, transfer.getTransferTime(fromTrip, toTrip2));
        
        // Add SpecificTransfer one route, specificity 1
        transfer.addSpecificTransfer(new SpecificTransfer(null, toRoute2.getId(), null, null, StopTransfer.PREFERRED_TRANSFER));
        assertEquals(StopTransfer.FORBIDDEN_TRANSFER, transfer.getTransferTime(fromTrip, toTrip));
        assertEquals(StopTransfer.PREFERRED_TRANSFER, transfer.getTransferTime(fromTrip, toTrip2));
        
        // Add SpecificTransfer one trip (and one ignored route), specificity 2
        transfer.addSpecificTransfer(new SpecificTransfer(null, toRoute2.getId(), null, toTrip2.getId(), StopTransfer.TIMED_TRANSFER));
        assertEquals(StopTransfer.FORBIDDEN_TRANSFER, transfer.getTransferTime(fromTrip, toTrip));
        assertEquals(StopTransfer.TIMED_TRANSFER, transfer.getTransferTime(fromTrip, toTrip2));
        
        // Add SpecificTransfer one trip and one route, specificity 3
        transfer.addSpecificTransfer(new SpecificTransfer(fromRoute.getId(), toRoute2.getId(), fromTrip.getId(), null, StopTransfer.UNKNOWN_TRANSFER));
        assertEquals(StopTransfer.FORBIDDEN_TRANSFER, transfer.getTransferTime(fromTrip, toTrip));
        assertEquals(StopTransfer.UNKNOWN_TRANSFER, transfer.getTransferTime(fromTrip, toTrip2));
        
        // Add SpecificTransfer one route, specificity 1
        transfer.addSpecificTransfer(new SpecificTransfer(fromRoute.getId(), null, null, null, 3));
        assertEquals(3, transfer.getTransferTime(fromTrip, toTrip));
        assertEquals(StopTransfer.UNKNOWN_TRANSFER, transfer.getTransferTime(fromTrip, toTrip2));
    }
}
