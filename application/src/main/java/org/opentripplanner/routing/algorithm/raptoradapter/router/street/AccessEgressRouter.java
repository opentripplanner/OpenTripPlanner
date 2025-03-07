package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.graph_builder.module.nearbystops.StreetNearbyStopFinder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.utils.collection.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This uses a street search to find paths to all the access/egress stop within range
 */
public class AccessEgressRouter {

  private static final Logger LOG = LoggerFactory.getLogger(AccessEgressRouter.class);

  private AccessEgressRouter() {}

  /**
   * Find accesses or egresses.
   */
  public static Collection<NearbyStop> findAccessEgresses(
    RouteRequest request,
    TemporaryVerticesContainer verticesContainer,
    StreetRequest streetRequest,
    @Nullable DataOverlayContext dataOverlayContext,
    AccessEgressType accessOrEgress,
    Duration durationLimit,
    int maxStopCount
  ) {
    OTPRequestTimeoutException.checkForTimeout();

    // Note: We calculate access/egresses in two parts. First we fetch the stops with zero distance.
    // Then we do street search. This is because some stations might use the centroid for street
    // routing, but should still give zero distance access/egresses to its child-stops.
    var zeroDistanceAccessEgress = findAccessEgressWithZeroDistance(
      verticesContainer,
      request,
      streetRequest,
      accessOrEgress
    );

    // When looking for street accesses/egresses we ignore the already found direct accesses/egresses
    var ignoreVertices = zeroDistanceAccessEgress
      .stream()
      .map(nearbyStop -> nearbyStop.state.getVertex())
      .collect(Collectors.toSet());

    var originVertices = accessOrEgress.isAccess()
      ? verticesContainer.getFromVertices()
      : verticesContainer.getToVertices();
    var streetAccessEgress = new StreetNearbyStopFinder(
      durationLimit,
      maxStopCount,
      dataOverlayContext,
      ignoreVertices
    ).findNearbyStops(originVertices, request, streetRequest, accessOrEgress.isEgress());

    var results = ListUtils.combine(zeroDistanceAccessEgress, streetAccessEgress);
    LOG.debug("Found {} {} stops", results.size(), accessOrEgress);
    return results;
  }

  /**
   * Return a list of direct accesses/egresses that do not require any street search. This will
   * return an empty list if the source/destination is not a stopId.
   */
  private static List<NearbyStop> findAccessEgressWithZeroDistance(
    TemporaryVerticesContainer verticesContainer,
    RouteRequest routeRequest,
    StreetRequest streetRequest,
    AccessEgressType accessOrEgress
  ) {
    var transitStopVertices = accessOrEgress.isAccess()
      ? verticesContainer.getFromStopVertices()
      : verticesContainer.getToStopVertices();

    return NearbyStop.nearbyStopsForTransitStopVerticesFiltered(
      transitStopVertices,
      accessOrEgress.isEgress(),
      routeRequest,
      streetRequest
    );
  }
}
