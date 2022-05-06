package org.opentripplanner.routing.algorithm;

import static org.opentripplanner.transit.model._data.TransitModelForTest.FEED_ID;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.organization.Agency;

/**
 * Test the banning and whitelisting functionality in the RoutingRequest.
 * TODO This does not test the that banning/whitelisting affects the routing correctly.
 */
public class TestBanning {

  @Test
  public void testSetBannedOnRequest() {
    Collection<Route> routes = getTestRoutes();

    RoutingRequest routingRequest = new RoutingRequest();

    routingRequest.setBannedRoutesFromString("F__RUT:Route:1");
    routingRequest.setBannedAgenciesFromSting("F:RUT:Agency:2");

    Collection<FeedScopedId> bannedRoutes = routingRequest.getBannedRoutes(routes);

    Assert.assertEquals(2, bannedRoutes.size());
    Assert.assertTrue(bannedRoutes.contains(new FeedScopedId(FEED_ID, "RUT:Route:1")));
    Assert.assertTrue(bannedRoutes.contains(new FeedScopedId(FEED_ID, "RUT:Route:3")));
  }

  @Test
  public void testSetWhiteListedOnRequest() {
    Collection<Route> routes = getTestRoutes();

    RoutingRequest routingRequest = new RoutingRequest();

    routingRequest.setWhiteListedRoutesFromString("F__RUT:Route:1");
    routingRequest.setWhiteListedAgenciesFromSting("F:RUT:Agency:2");

    Collection<FeedScopedId> bannedRoutes = routingRequest.getBannedRoutes(routes);

    Assert.assertEquals(1, bannedRoutes.size());
    Assert.assertTrue(bannedRoutes.contains(new FeedScopedId(FEED_ID, "RUT:Route:2")));
  }

  private Collection<Route> getTestRoutes() {
    Route route1 = new Route(new FeedScopedId(FEED_ID, "RUT:Route:1"));
    route1.setLongName("");
    Route route2 = new Route(new FeedScopedId(FEED_ID, "RUT:Route:2"));
    route2.setLongName("");
    Route route3 = new Route(new FeedScopedId(FEED_ID, "RUT:Route:3"));
    route3.setLongName("");

    Agency agency1 = TransitModelForTest
      .agency("A")
      .mutate()
      .setId(TransitModelForTest.id("RUT:Agency:1"))
      .build();
    Agency agency2 = TransitModelForTest
      .agency("B")
      .mutate()
      .setId(TransitModelForTest.id("RUT:Agency:2"))
      .build();

    route1.setAgency(agency1);
    route2.setAgency(agency1);
    route3.setAgency(agency2);

    return Arrays.asList(route1, route2, route3);
  }
}
