package org.opentripplanner.routing.core;

import junit.framework.TestCase;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

public class TestSpecificTransfer extends TestCase {

    /**
     * Test different specific transfers
     */
    public void testSpecificTransfer() {
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
        
        // Create full SpecificTransfer
        SpecificTransfer s1 = new SpecificTransfer(fromRoute.getId(), toRoute.getId(), fromTrip.getId(), toTrip.getId(), 1);
        assertTrue(s1.matches(fromTrip, toTrip));
        assertTrue(s1.getSpecificity() == SpecificTransfer.MAX_SPECIFICITY);
        assertTrue(s1.transferTime == 1);
        
        // Create empty SpecificTransfer
        SpecificTransfer s2 = new SpecificTransfer((FeedScopedId) null, null, null, null, 2);
        assertTrue(s2.matches(fromTrip, toTrip));
        assertTrue(s2.getSpecificity() == SpecificTransfer.MIN_SPECIFICITY);
        assertTrue(s2.transferTime == 2);
        
        // Create SpecificTransfer one trip missing
        SpecificTransfer s3 = new SpecificTransfer(fromRoute.getId(), toRoute.getId(), null, toTrip.getId(), 3);
        assertTrue(s3.matches(fromTrip, toTrip));
        assertTrue(s3.getSpecificity() == 3);
        assertTrue(s3.transferTime == 3);
        
        // Create SpecificTransfer one trip different
        SpecificTransfer s4 = new SpecificTransfer(fromRoute.getId(), toRoute.getId(), new FeedScopedId("A1", "T3"), toTrip.getId(), 4);
        assertFalse(s4.matches(fromTrip, toTrip));
        assertTrue(s4.getSpecificity() == SpecificTransfer.MAX_SPECIFICITY);
        assertTrue(s4.transferTime == 4);
        
        // Create SpecificTransfer one trip and route missing
        SpecificTransfer s5 = new SpecificTransfer(null, toRoute.getId(), null, toTrip.getId(), 5);
        assertTrue(s5.matches(fromTrip, toTrip));
        assertTrue(s5.getSpecificity() == 2);
        assertTrue(s5.transferTime == 5);
        
        // Create SpecificTransfer one trip only
        SpecificTransfer s6 = new SpecificTransfer(null, null, null, toTrip.getId(), 6);
        assertTrue(s6.matches(fromTrip, toTrip));
        assertTrue(s6.getSpecificity() == 2);
        assertTrue(s6.transferTime == 6);
        
        // Create SpecificTransfer one route only
        SpecificTransfer s7 = new SpecificTransfer(fromRoute.getId(), null, null, null, 7);
        assertTrue(s7.matches(fromTrip, toTrip));
        assertTrue(s7.getSpecificity() == 1);
        assertTrue(s7.transferTime == 7);
    }
}
