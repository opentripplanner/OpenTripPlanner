package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.opentripplanner.apis.gtfs.mapping.CoordinateMapper.mapCoordinate;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.api.request.via.PassThroughViaLocation;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.collection.ListUtils;

/**
 * Maps the input data to the data structure needed for via routing.
 */
class ViaLocationMapper {

  static List<ViaLocation> mapToViaLocations(@Nullable List<Map<String, Object>> via) {
    return ListUtils.nullSafeImmutableList(via)
      .stream()
      .map(ViaLocationMapper::mapViaLocation)
      .toList();
  }

  private static ViaLocation mapViaLocation(Map<String, Object> via) {
    var viaInput = new GraphQLTypes.GraphQLPlanViaLocationInput(via);
    var passThrough = viaInput.getGraphQLPassThrough();
    var visit = viaInput.getGraphQLVisit();

    if (passThrough != null && passThrough.getGraphQLStopLocationIds() != null) {
      return new PassThroughViaLocation(
        passThrough.getGraphQLLabel(),
        mapStopLocationIds(passThrough.getGraphQLStopLocationIds())
      );
    } else if (visit != null) {
      return new VisitViaLocation(
        visit.getGraphQLLabel(),
        visit.getGraphQLMinimumWaitTime(),
        mapStopLocationIds(visit.getGraphQLStopLocationIds()),
        mapCoordinate(visit.getGraphQLCoordinate()).map(List::of).orElse(List.of())
      );
    } else {
      throw new IllegalArgumentException("ViaLocation must define either pass-through or visit.");
    }
  }

  private static List<FeedScopedId> mapStopLocationIds(@Nullable List<String> ids) {
    if (ids == null) {
      return List.of();
    }
    return ids.stream().map(FeedScopedId::parse).toList();
  }
}
