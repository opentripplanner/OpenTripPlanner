package org.opentripplanner.apis.gtfs.mapping.routerequest;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLPlanViaLocationInput;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.routing.api.request.via.PassThroughViaLocation;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Maps the input data to the data structure needed for via routing.
 */
class ViaLocationMapper {

  static List<ViaLocation> mapToViaLocations(@Nullable List<GraphQLPlanViaLocationInput> via) {
    return ListUtils.nullSafeImmutableList(via).stream().map(ViaLocationMapper::mapViaLocation).toList();
  }

  private static ViaLocation mapViaLocation(GraphQLPlanViaLocationInput via) {
    var passThrough = via.getGraphQLPassThrough();
    var visit = via.getGraphQLVisit();

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
        List.of()
      );
    } else {
      throw new IllegalArgumentException("ViaLocation must define either pass-through or visit.");
    }
  }

  private static List<FeedScopedId> mapStopLocationIds(List<String> ids) {
    return ids.stream().map(FeedScopedId::parse).toList();
  }
}
