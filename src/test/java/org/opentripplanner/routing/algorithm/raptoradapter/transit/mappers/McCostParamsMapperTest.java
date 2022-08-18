package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class McCostParamsMapperTest {

  static FeedScopedId regularAgency = id("regular-agency");
  static FeedScopedId unpreferredAgency = id("unpreferred-agency");
  static FeedScopedId agencyWithNoRoutes = id("agency-without-routes");

  static FeedScopedId route1 = id("route1");
  static FeedScopedId route2 = id("route2");
  static FeedScopedId route3 = id("route3");
  static Multimap<FeedScopedId, FeedScopedId> routesByAgencies = ArrayListMultimap.create();

  static {
    routesByAgencies.putAll(regularAgency, List.of(route1));
    routesByAgencies.putAll(unpreferredAgency, List.of(route2, route3));
  }

  @Test
  public void shouldExtractRoutesFromAgencies() {
    var routingRequest = new RoutingRequest();
    routingRequest.setUnpreferredAgencies(List.of(unpreferredAgency));

    assertEquals(
      //TODO
      new BitSet(),
      McCostParamsMapper.map(routingRequest, List.of()).unpreferredPatterns()
    );

    routingRequest.setUnpreferredAgencies(List.of(agencyWithNoRoutes));
  }

  @Test
  public void dealWithEmptyList() {
    var routingRequest = new RoutingRequest();
    routingRequest.setUnpreferredAgencies(List.of(agencyWithNoRoutes));

    assertEquals(
      //TODO
      new BitSet(),
      McCostParamsMapper.map(routingRequest, List.of()).unpreferredPatterns()
    );
  }
}
