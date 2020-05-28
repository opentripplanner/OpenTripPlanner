package org.opentripplanner.routing.core;

import org.junit.Test;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.api.request.RoutingRequest;

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
        assertTrue(request.streetSubRequestModes.getCar());
        request.removeMode(CAR);
        assertFalse(request.streetSubRequestModes.getCar());

        request.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.BICYCLE,TraverseMode.WALK));
        assertFalse(request.streetSubRequestModes.getCar());
        assertTrue(request.streetSubRequestModes.getBicycle());
        assertTrue(request.streetSubRequestModes.getWalk());
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
        FeedScopedId id = new FeedScopedId("feedId", "1");
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
