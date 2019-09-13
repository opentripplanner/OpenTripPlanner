package org.opentripplanner.routing.core;

import org.junit.Test;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.common.model.GenericLocation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.routing.core.TraverseMode.CAR;

public class RoutingRequestTest {

    private GenericLocation randomLocation() {
        return new GenericLocation(Math.random(), Math.random());
    }

    @Test
    public void testRequest() {
        RoutingRequest request = new RoutingRequest();

        request.addMode(CAR);
        assertTrue(request.modes.getCar());
        request.removeMode(CAR);
        assertFalse(request.modes.getCar());

        request.setModes(new TraverseModeSet("BICYCLE,WALK"));
        assertFalse(request.modes.getCar());
        assertTrue(request.modes.getBicycle());
        assertTrue(request.modes.getWalk());
    }

    @Test
    public void testIntermediatePlaces() {
        RoutingRequest req = new RoutingRequest();
        assertFalse(req.hasIntermediatePlaces());

        req.clearIntermediatePlaces();
        assertFalse(req.hasIntermediatePlaces());

        req.addIntermediatePlace(randomLocation());
        assertTrue(req.hasIntermediatePlaces());
        
        req.clearIntermediatePlaces();
        assertFalse(req.hasIntermediatePlaces());

        req.addIntermediatePlace(randomLocation());
        req.addIntermediatePlace(randomLocation());
        assertTrue(req.hasIntermediatePlaces());
    }

    @Test
    public void testPreferencesPenaltyForRoute() {
        FeedScopedId id = new FeedScopedId();
        Agency agency = new Agency();
        Route route = new Route();
        Trip trip = new Trip();
        RoutingRequest routingRequest = new RoutingRequest();

        trip.setRoute(route);
        route.setId(id);
        route.setAgency(agency);
        assertEquals(0, routingRequest.preferencesPenaltyForRoute(trip.getRoute()));
    }
}
