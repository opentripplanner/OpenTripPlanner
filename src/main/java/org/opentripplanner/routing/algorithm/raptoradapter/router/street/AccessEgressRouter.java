package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.graph_builder.module.nearbystops.DirectlyConnectedStopFinder;
import org.opentripplanner.graph_builder.module.nearbystops.StreetNearbyStopFinder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.index.StreetIndex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestMapper;
import org.opentripplanner.street.search.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This uses a street search to find paths to all the access/egress stop within range
 */
public class AccessEgressRouter {

  private static final Logger LOG = LoggerFactory.getLogger(AccessEgressRouter.class);

  private AccessEgressRouter() {}

  /**
   * Search for accesses or egresses. This will return both:
   * <li> Directly accessible stops if the source/destination is a stop id.
   * <li> Stops reachable by street search from the coordinates that correspond to the source/destination.
   *
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

    // Note: We do direct and street search in two parts since some stations will use the centroid
    // for street routing, but should still give direct access/egresses to its child-stops.
    var directAccessEgress = AccessEgressRouter.findDirectAccessEgress(
      verticesContainer,
      request,
      streetRequest,
      accessOrEgress
    );

    // When looking for street accesses/egresses we ignore the already found direct accesses/egresses
    var ignoreVertices = directAccessEgress
      .stream()
      .map(nearbyStop -> nearbyStop.state.getVertex())
      .collect(Collectors.toSet());

    var originVertices = accessOrEgress.isEgress()
      ? verticesContainer.getToVertices()
      : verticesContainer.getFromVertices();
    var streetAccessEgress = new StreetNearbyStopFinder(
      durationLimit,
      maxStopCount,
      dataOverlayContext,
      ignoreVertices
    )
      .findNearbyStops(originVertices, request, streetRequest, accessOrEgress.isEgress());

    var results = ListUtils.combine(directAccessEgress, streetAccessEgress);
    LOG.debug("Found {} {} stops", results.size(), accessOrEgress);
    return results;
  }

  /**
   * Return a list of direct accesses/egresses that do not require any street search. This will
   * return an empty list if the source/destination is not a stopId.
   */
  private static List<NearbyStop> findDirectAccessEgress(
    TemporaryVerticesContainer verticesContainer,
    RouteRequest routeRequest,
    StreetRequest streetRequest,
    AccessEgressType accessOrEgress
  ) {
    var directVertices = accessOrEgress.isEgress()
      ? verticesContainer.getToStopVertices()
      : verticesContainer.getFromStopVertices();

    if (directVertices.isEmpty()) {
      return List.of();
    }

    return DirectlyConnectedStopFinder.findDirectlyConnectedStops(
      Collections.unmodifiableSet(directVertices),
      accessOrEgress.isEgress(),
      routeRequest,
      streetRequest
    );
  }
}
