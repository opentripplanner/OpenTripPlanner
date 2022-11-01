package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RouteRequestTransitDataProviderFilter;
import org.opentripplanner.routing.api.request.request.TransitRequest;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;

/**
 * Test the banning and whitelisting functionality in the RouteRequest.
 * TODO This does not test the that banning/whitelisting affects the routing correctly.
 */
public class TestBanning {

  @Test
  public void testSetBannedOnRequest() {
    Collection<Route> routes = getTestRoutes();

    final TransitRequest transit = new TransitRequest();
    transit.setBannedRoutes(RouteMatcher.parse("F__RUT:Route:1"));
    transit.setBannedAgencies(List.of(FeedScopedId.parseId("F:RUT:Agency:2")));

    Collection<FeedScopedId> bannedRoutes = RouteRequestTransitDataProviderFilter.bannedRoutes(
      Set.copyOf(transit.bannedAgencies()),
      transit.bannedRoutes(),
      Set.copyOf(transit.whiteListedAgencies()),
      transit.whiteListedRoutes(),
      routes
    );

    assertEquals(2, bannedRoutes.size());
    assertTrue(bannedRoutes.contains(id("RUT:Route:1")));
    assertTrue(bannedRoutes.contains(id("RUT:Route:3")));
  }

  @Test
  public void testSetWhiteListedOnRequest() {
    Collection<Route> routes = getTestRoutes();

    TransitRequest transit = new TransitRequest();
    transit.setWhiteListedRoutes(RouteMatcher.parse("F__RUT:Route:1"));
    transit.setWhiteListedAgencies(List.of(FeedScopedId.parseId("F:RUT:Agency:2")));

    Collection<FeedScopedId> bannedRoutes = RouteRequestTransitDataProviderFilter.bannedRoutes(
      Set.copyOf(transit.bannedAgencies()),
      transit.bannedRoutes(),
      Set.copyOf(transit.whiteListedAgencies()),
      transit.whiteListedRoutes(),
      routes
    );

    assertEquals(1, bannedRoutes.size());
    assertTrue(bannedRoutes.contains(id("RUT:Route:2")));
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
