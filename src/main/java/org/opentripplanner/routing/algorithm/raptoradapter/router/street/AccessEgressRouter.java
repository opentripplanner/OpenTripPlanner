package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.TemporaryVerticesContainer;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This uses a street search to find paths to all the access/egress stop within range
 */
public class AccessEgressRouter {

  private static final Logger LOG = LoggerFactory.getLogger(AccessEgressRouter.class);

  private AccessEgressRouter() {}

  /**
   * @param fromTarget whether to route from or towards the point provided in the routing request
   *                   (access or egress)
   * @return Transfer objects by access/egress stop
   */
  public static Collection<NearbyStop> streetSearch(
    RouteRequest request,
    TemporaryVerticesContainer verticesContainer,
    TransitService transitService,
    StreetMode streetMode,
    DataOverlayContext dataOverlayContext,
    boolean fromTarget
  ) {
    //TODO: Investigate why this is needed for flex
    RouteRequest nearbyRequest = request.getStreetSearchRequest(streetMode);

    NearbyStopFinder nearbyStopFinder = new NearbyStopFinder(
      transitService,
      nearbyRequest.preferences().street().maxAccessEgressDuration().valueOf(streetMode),
      dataOverlayContext,
      true
    );
    List<NearbyStop> nearbyStopList = nearbyStopFinder.findNearbyStopsViaStreets(
      fromTarget ? verticesContainer.getToVertices() : verticesContainer.getFromVertices(),
      fromTarget,
      nearbyRequest
    );

    LOG.debug("Found {} {} stops", nearbyStopList.size(), fromTarget ? "egress" : "access");

    return nearbyStopList;
  }
}
