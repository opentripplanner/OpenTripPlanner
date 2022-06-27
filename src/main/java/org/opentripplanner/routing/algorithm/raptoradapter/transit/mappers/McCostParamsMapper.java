package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.McCostParams;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.McCostParamsBuilder;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TransitMode;

public class McCostParamsMapper {

  public static McCostParams map(RoutingRequest request) {
    McCostParamsBuilder builder = new McCostParamsBuilder();

    builder.transferCost(request.transferCost).waitReluctanceFactor(request.waitReluctance);

    if (request.modes.transferMode == StreetMode.BIKE) {
      builder.boardCost(request.bikeBoardCost);
    } else {
      builder.boardCost(request.walkBoardCost);
    }
    builder.transitReluctanceFactors(mapTransitReluctance(request.transitReluctanceForMode()));

    builder.wheelchairAccessibility(request.wheelchairAccessibility);

    builder.routePenalties(
      buildRoutePenalties(request.unpreferredRoutes, request.useUnpreferredRoutesPenalty)
    );

    return builder.build();
  }

  public static double[] mapTransitReluctance(Map<TransitMode, Double> map) {
    if (map.isEmpty()) {
      return null;
    }

    // The transit reluctance is arranged in an array with the {@link TransitMode} ordinal
    // as an index. This make the lookup very fast and the size of the array small.
    // We could get away with a smaller array if we kept an index from mode to index
    // and passed that into the transit layer and used it to set the
    // {@link TripScheduleWithOffset#transitReluctanceIndex}, but this is difficult with the
    // current transit model design.
    double[] transitReluctance = new double[TransitMode.values().length];
    Arrays.fill(transitReluctance, McCostParams.DEFAULT_TRANSIT_RELUCTANCE);
    for (TransitMode mode : map.keySet()) {
      transitReluctance[mode.ordinal()] = map.get(mode);
    }
    return transitReluctance;
  }

  /**
   * Build a lookup table of routes that should be penalized in search by unpreference cost.
   * @param routeIds
   * @param penalty
   * @return lookup table of penalties by routeId
   */
  private static Map<FeedScopedId, Integer> buildRoutePenalties(
    List<FeedScopedId> routeIds,
    Integer penalty
  ) {
    var map = new HashMap<FeedScopedId, Integer>();
    for (FeedScopedId routeId : routeIds) {
      map.put(routeId, RaptorCostConverter.toRaptorCost(penalty));
    }
    return map;
  }
}
