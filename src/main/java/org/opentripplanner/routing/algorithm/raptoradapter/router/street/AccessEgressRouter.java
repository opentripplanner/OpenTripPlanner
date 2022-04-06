package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This uses a street search to find paths to all the access/egress stop within range
 */
public class AccessEgressRouter {

  private static final Logger LOG = LoggerFactory.getLogger(AccessEgressRouter.class);

  private AccessEgressRouter() {}

  /**
   *
   * @param rctx the current routing context
   * @param fromTarget whether to route from or towards the point provided in the routing request
   *                   (access or egress)
   * @return Transfer objects by access/egress stop
   */
  public static Collection<NearbyStop> streetSearch(
    RoutingContext rctx,
    StreetMode streetMode,
    boolean fromTarget
  ) {
    final RoutingRequest rr = rctx.opt;
    Set<Vertex> vertices = fromTarget != rr.arriveBy ? rctx.toVertices : rctx.fromVertices;

    //TODO: Investigate why this is needed for flex
    RoutingRequest nearbyRequest = rr.getStreetSearchRequest(streetMode);

    NearbyStopFinder nearbyStopFinder = new NearbyStopFinder(
      rctx.graph,
      rr.getMaxAccessEgressDuration(streetMode),
      true
    );
    List<NearbyStop> nearbyStopList = nearbyStopFinder.findNearbyStopsViaStreets(
      vertices,
      fromTarget,
      nearbyRequest
    );

    LOG.debug("Found {} {} stops", nearbyStopList.size(), fromTarget ? "egress" : "access");

    return nearbyStopList;
  }
}
