package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.place.nearbystopfinder.StreetNearbyStopFinder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.ExtensionRequestContext;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.service.StreetLimitationParametersService;

/**
 * This uses a street search to find paths to all the access/egress stop within range. Doesn't
 * support routing through via locations.
 */
public class DefaultAccessEgressRouter extends AccessEgressRouter {

  @Override
  Collection<NearbyStop> findStreetAccessEgresses(
    RouteRequest request,
    StreetMode streetMode,
    Collection<ExtensionRequestContext> extensionRequestContexts,
    AccessEgressType accessOrEgress,
    Duration durationLimit,
    int maxStopCount,
    LinkingContext linkingContext,
    Set<Vertex> ignoreVertices,
    StreetLimitationParametersService streetLimitationParametersService,
    GeofencingZoneService geofencingZoneService
  ) {
    var originVertices = accessOrEgress.isAccess()
      ? linkingContext.findVertices(request.from())
      : linkingContext.findVertices(request.to());
    return StreetNearbyStopFinder.of(null)
      .withIgnoreVertices(ignoreVertices)
      .withExtensionRequestContexts(extensionRequestContexts)
      .build()
      .findNearbyStops(
        originVertices,
        request,
        streetMode,
        accessOrEgress.isEgress(),
        durationLimit,
        maxStopCount
      );
  }
}
