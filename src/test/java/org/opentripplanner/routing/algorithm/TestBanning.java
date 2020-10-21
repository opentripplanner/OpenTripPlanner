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

        routingRequest.setBannedRoutesFromSting("RB__RUT:Route:1");
        routingRequest.setBannedAgenciesFromSting("RB:RUT:Agency:2");

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

        routingRequest.setWhiteListedRoutesFromSting("RB__RUT:Route:1");
        routingRequest.setWhiteListedAgenciesFromSting("RB:RUT:Agency:2");

        Collection<FeedScopedId> bannedRoutes =
            routingRequest.getBannedRoutes(routes);

        Assert.assertEquals(1, bannedRoutes.size());
        Assert.assertTrue(bannedRoutes.contains(new FeedScopedId("RB", "RUT:Route:2")));
    }

    private Collection<Route> getTestRoutes() {
        Route route1 = new Route(new FeedScopedId("RB", "RUT:Route:1"));
        route1.setLongName("");
        Route route2 = new Route(new FeedScopedId("RB", "RUT:Route:2"));
        route2.setLongName("");
        Route route3 = new Route(new FeedScopedId("RB", "RUT:Route:3"));
        route3.setLongName("");

        Agency agency1 = new Agency(
            new FeedScopedId("RB", "RUT:Agency:1"), "A", "Europe/Paris"
        );
        Agency agency2 = new Agency(
            new FeedScopedId("RB", "RUT:Agency:2"), "B", "Europe/Paris"
        );

        route1.setAgency(agency1);
        route2.setAgency(agency1);
        route3.setAgency(agency2);

        return Arrays.asList(route1, route2, route3);
    }
}
