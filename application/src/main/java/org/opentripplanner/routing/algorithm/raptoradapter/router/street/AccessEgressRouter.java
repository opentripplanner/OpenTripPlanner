package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.place.nearbystopfinder.NearbyStopFactory;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.ExtensionRequestContext;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.utils.collection.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for a street search to find paths to all the access/egress stop within range.
 * Follows template method pattern.
 */
public abstract class AccessEgressRouter {

  private static final Logger LOG = LoggerFactory.getLogger(AccessEgressRouter.class);

  /**
   * Find accesses or egresses.
   */
  public Collection<NearbyStop> findAccessEgresses(
    RouteRequest request,
    StreetMode streetMode,
    Collection<ExtensionRequestContext> extensionRequestContexts,
    AccessEgressType accessOrEgress,
    Duration durationLimit,
    int maxStopCount,
    LinkingContext linkingContext,
    StreetLimitationParametersService streetLimitationParametersService,
    GeofencingZoneService geofencingZoneService
  ) {
    OTPRequestTimeoutException.checkForTimeout();

    // Note: We calculate access/egresses in two parts. First we fetch the stops with zero distance.
    // Then we do street search. This is because some stations might use the centroid for street
    // routing, but should still give zero distance access/egresses to its child-stops.
    var zeroDistanceAccessEgress = findAccessEgressWithZeroDistance(
      request,
      streetMode,
      accessOrEgress,
      linkingContext
    );

    // When looking for street accesses/egresses we ignore the already found direct accesses/egresses
    var ignoreVertices = zeroDistanceAccessEgress
      .stream()
      .map(nearbyStop -> nearbyStop.finalStates.getLast().getVertex())
      .collect(Collectors.toSet());

    var streetAccessEgress = findStreetAccessEgresses(
      request,
      streetMode,
      extensionRequestContexts,
      accessOrEgress,
      durationLimit,
      maxStopCount,
      linkingContext,
      ignoreVertices,
      streetLimitationParametersService,
      geofencingZoneService
    );

    var results = ListUtils.combine(zeroDistanceAccessEgress, streetAccessEgress);
    LOG.debug("Found {} {} stops", results.size(), accessOrEgress);
    return results;
  }

  /**
   * Find accesses or egresses using street routing.
   */
  abstract Collection<NearbyStop> findStreetAccessEgresses(
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
  );

  /**
   * Return a list of direct accesses/egresses that do not require any street search. This will
   * return an empty list if the source/destination is not a stopId.
   */
  private List<NearbyStop> findAccessEgressWithZeroDistance(
    RouteRequest routeRequest,
    StreetMode streetMode,
    AccessEgressType accessOrEgress,
    LinkingContext linkingContext
  ) {
    var transitStopVertices = accessOrEgress.isAccess()
      ? linkingContext.fromStopVertices()
      : linkingContext.toStopVertices();

    return NearbyStopFactory.nearbyStopsForTransitStopVerticesFiltered(
      transitStopVertices,
      accessOrEgress.isEgress(),
      routeRequest,
      streetMode
    );
  }
}
