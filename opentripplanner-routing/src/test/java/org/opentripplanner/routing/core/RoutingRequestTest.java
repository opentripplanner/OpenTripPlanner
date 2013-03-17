package org.opentripplanner.routing.core;

import static org.junit.Assert.*;

import org.junit.Test;
import org.opentripplanner.common.model.GenericLocation;

public class RoutingRequestTest {

    private GenericLocation randomLocation() {
        return new GenericLocation(Math.random(), Math.random());
    }
    
    @Test
    public void testIntermediatePlaces() {
        RoutingRequest req = new RoutingRequest();
        assertFalse(req.hasIntermediatePlaces());
        assertFalse(req.intermediatesEffectivelyOrdered());
        
        req.clearIntermediatePlaces();
        assertFalse(req.hasIntermediatePlaces());
        assertFalse(req.intermediatesEffectivelyOrdered());
        
        req.addIntermediatePlace(randomLocation());
        assertTrue(req.hasIntermediatePlaces());
        
        // There is only one intermediate, so they are effectively ordered.
        assertTrue(req.intermediatesEffectivelyOrdered());
        
        req.clearIntermediatePlaces();
        assertFalse(req.hasIntermediatePlaces());
        assertFalse(req.intermediatesEffectivelyOrdered());
        
        req.addIntermediatePlace(randomLocation());
        req.addIntermediatePlace(randomLocation());
        assertTrue(req.hasIntermediatePlaces());
        assertFalse(req.intermediatesEffectivelyOrdered());
        
        req.setIntermediatePlacesOrdered(true);
        assertTrue(req.intermediatesEffectivelyOrdered());        
    }

}
