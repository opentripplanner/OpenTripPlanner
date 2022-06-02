package org.opentripplanner.routing.algorithm;

import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.Collection;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
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
    Assert.assertTrue(bannedRoutes.contains(id("RUT:Route:1")));
    Assert.assertTrue(bannedRoutes.contains(id("RUT:Route:3")));
  }

  @Test
  public void testSetWhiteListedOnRequest() {
    Collection<Route> routes = getTestRoutes();

    RoutingRequest routingRequest = new RoutingRequest();

    routingRequest.setWhiteListedRoutesFromString("F__RUT:Route:1");
    routingRequest.setWhiteListedAgenciesFromSting("F:RUT:Agency:2");

    Collection<FeedScopedId> bannedRoutes = routingRequest.getBannedRoutes(routes);

    Assert.assertEquals(1, bannedRoutes.size());
    Assert.assertTrue(bannedRoutes.contains(id("RUT:Route:2")));
  }

  private List<Route> getTestRoutes() {
    Agency agency1 = TransitModelForTest.agency("A").copy().withId(id("RUT:Agency:1")).build();
    Agency agency2 = TransitModelForTest.agency("B").copy().withId(id("RUT:Agency:2")).build();

    return List.of(
      TransitModelForTest.route("RUT:Route:1").withAgency(agency1).build(),
      TransitModelForTest.route("RUT:Route:2").withAgency(agency1).build(),
      TransitModelForTest.route("RUT:Route:3").withAgency(agency2).build()
    );
  }
}
