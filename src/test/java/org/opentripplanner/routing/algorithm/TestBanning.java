package org.opentripplanner.routing.algorithm;

import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.api.request.RoutingRequest;

import java.util.Arrays;
import java.util.Collection;

/**
 * Test the banning and whitelisting functionality in the RoutingRequest.
 * TODO This does not test the that banning/whitelisting affects the routing correctly.
 */
public class TestBanning {

    @Test
    public void testSetBannedOnRequest() {
        Collection<Route> routes = getTestRoutes();

        RoutingRequest routingRequest = new RoutingRequest();

        routingRequest.setBannedRoutes("RB__RUT:Route:1");
        routingRequest.setBannedAgencies("RB:RUT:Agency:2");

        Collection<FeedScopedId> bannedRoutes =
            routingRequest.getBannedRoutes(routes);

        Assert.assertEquals(2, bannedRoutes.size());
        Assert.assertTrue(bannedRoutes.contains(new FeedScopedId("RB", "RUT:Route:1")));
        Assert.assertTrue(bannedRoutes.contains(new FeedScopedId("RB", "RUT:Route:3")));
    }

    @Test
    public void testSetWhiteListedOnRequest() {
        Collection<Route> routes = getTestRoutes();

        RoutingRequest routingRequest = new RoutingRequest();

        routingRequest.setWhiteListedRoutes("RB__RUT:Route:1");
        routingRequest.setWhiteListedAgencies("RB:RUT:Agency:2");

        Collection<FeedScopedId> bannedRoutes =
            routingRequest.getBannedRoutes(routes);

        Assert.assertEquals(1, bannedRoutes.size());
        Assert.assertTrue(bannedRoutes.contains(new FeedScopedId("RB", "RUT:Route:2")));
    }

    private Collection<Route> getTestRoutes() {
        Route route1 = new Route();
        route1.setId(new FeedScopedId("RB", "RUT:Route:1"));
        route1.setLongName("");
        Route route2 = new Route();
        route2.setId(new FeedScopedId("RB", "RUT:Route:2"));
        route2.setLongName("");
        Route route3 = new Route();
        route3.setId(new FeedScopedId("RB", "RUT:Route:3"));
        route3.setLongName("");

        Agency agency1 = new Agency();
        agency1.setId(new FeedScopedId("RB", "RUT:Agency:1"));
        Agency agency2 = new Agency();
        agency2.setId(new FeedScopedId("RB", "RUT:Agency:2"));

        route1.setAgency(agency1);
        route2.setAgency(agency1);
        route3.setAgency(agency2);

        return Arrays.asList(route1, route2, route3);
    }
}
