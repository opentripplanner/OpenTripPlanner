package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.graph_builder.module.nearbystops.StreetNearbyStopFinder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.NearbyStopFactory;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.ExtensionRequestContext;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.streetadapter.StreetSearchRequestMapper;
import org.opentripplanner.utils.collection.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This uses a street search to find paths to all the access/egress stop within range
 */
public class AccessEgressRouter {

  private static final Logger LOG = LoggerFactory.getLogger(AccessEgressRouter.class);

  /**
   * Find accesses or egresses.
   */
  public static Collection<NearbyStop> findAccessEgresses(
    RouteRequest request,
    StreetMode streetMode,
    Collection<ExtensionRequestContext> extensionRequestContexts,
    AccessEgressType accessOrEgress,
    Duration durationLimit,
    int maxStopCount,
    LinkingContext linkingContext
  ) {
    OTPRequestTimeoutException.checkForTimeout();

    // Note: We calculate access/egresses in two parts. First we fetch the stops with zero distance.
    // Then we do street search. This is because some stations might use the centroid for street
    // routing, but should still give zero distance access/egresses to its child-stops.
    var zeroDistanceRequest = StreetSearchRequestMapper.mapToTransferRequest(request)
      .withArriveBy(accessOrEgress.isEgress())
      .withMode(streetMode)
      .build();
    var zeroDistanceAccessEgress = findAccessEgressWithZeroDistance(
      zeroDistanceRequest,
      accessOrEgress,
      linkingContext
    );

    // When looking for street accesses/egresses we ignore the already found direct accesses/egresses
    var ignoreVertices = zeroDistanceAccessEgress
      .stream()
      .map(nearbyStop -> nearbyStop.state.getVertex())
      .collect(Collectors.toSet());

    var originVertices = accessOrEgress.isAccess()
      ? linkingContext.findVertices(request.from())
      : linkingContext.findVertices(request.to());
    var accessEgressRequest = StreetSearchRequestMapper.map(request)
      .withArriveBy(accessOrEgress.isEgress())
      .withMode(streetMode)
      .withExtensionRequestContexts(extensionRequestContexts)
      .build();
    var streetAccessEgress = StreetNearbyStopFinder.of(durationLimit, maxStopCount)
      .withIgnoreVertices(ignoreVertices)
      .build()
      .findNearbyStops(originVertices, accessEgressRequest);

    var results = ListUtils.combine(zeroDistanceAccessEgress, streetAccessEgress);
    LOG.debug("Found {} {} stops", results.size(), accessOrEgress);
    return results;
  }

  /**
   * Return a list of direct accesses/egresses that do not require any street search. This will
   * return an empty list if the source/destination is not a stopId.
   */
  private static List<NearbyStop> findAccessEgressWithZeroDistance(
    StreetSearchRequest streetSearchRequest,
    AccessEgressType accessOrEgress,
    LinkingContext linkingContext
  ) {
    var transitStopVertices = accessOrEgress.isAccess()
      ? linkingContext.fromStopVertices()
      : linkingContext.toStopVertices();

    return NearbyStopFactory.nearbyStopsForTransitStopVerticesFiltered(
      transitStopVertices,
      streetSearchRequest
    );
  }
}
