package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.agency;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.route;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.BitSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptorlegacy._data.transit.TestRoute;
import org.opentripplanner.raptorlegacy._data.transit.TestTransitData;
import org.opentripplanner.raptorlegacy._data.transit.TestTripPattern;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class GeneralizedCostParametersMapperTest {

  static FeedScopedId regularAgency = id("regular-agency");
  static FeedScopedId unpreferredAgency = id("unpreferred-agency");
  static FeedScopedId agencyWithNoRoutes = id("agency-without-routes");

  static FeedScopedId route1 = id("route1");
  static FeedScopedId route2 = id("route2");
  static FeedScopedId route3 = id("route3");
  static Multimap<FeedScopedId, FeedScopedId> routesByAgencies = ArrayListMultimap.create();
  static TestTransitData data;

  static {
    routesByAgencies.putAll(regularAgency, List.of(route1));
    routesByAgencies.putAll(unpreferredAgency, List.of(route2, route3));
    data = new TestTransitData();

    for (var it : routesByAgencies.entries()) {
      data.withRoute(testTripPattern(it.getKey(), it.getValue()));
    }
  }

  @Test
  public void shouldExtractRoutesFromAgencies() {
    var routingRequest = new RouteRequest();
    routingRequest.journey().transit().setUnpreferredAgencies(List.of(unpreferredAgency));

    BitSet unpreferredPatterns = GeneralizedCostParametersMapper.map(
      routingRequest,
      data.getPatterns()
    ).unpreferredPatterns();

    for (var pattern : data.getPatterns()) {
      assertEquals(
        pattern.route().getAgency().getId().equals(unpreferredAgency),
        unpreferredPatterns.get(pattern.patternIndex())
      );
    }
  }

  @Test
  public void dealWithEmptyList() {
    var routingRequest = new RouteRequest();
    routingRequest.journey().transit().setUnpreferredAgencies(List.of(agencyWithNoRoutes));

    assertEquals(
      new BitSet(),
      GeneralizedCostParametersMapper.map(routingRequest, data.getPatterns()).unpreferredPatterns()
    );
  }

  private static TestRoute testTripPattern(FeedScopedId agencyId, FeedScopedId routeId) {
    return TestRoute.route(
      TestTripPattern.pattern(1, 2).withRoute(
        route(routeId).withAgency(agency(agencyId.getId())).build()
      )
    );
  }
}
