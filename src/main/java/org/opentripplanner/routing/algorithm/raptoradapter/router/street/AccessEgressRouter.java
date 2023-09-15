package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
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
    StreetRequest streetRequest,
    DataOverlayContext dataOverlayContext,
    boolean fromTarget,
    Duration durationLimit,
    int maxStopCount
  ) {
    OTPRequestTimeoutException.checkForTimeout();
    NearbyStopFinder nearbyStopFinder = new NearbyStopFinder(
      transitService,
      durationLimit,
      maxStopCount,
      dataOverlayContext,
      true
    );
    List<NearbyStop> nearbyStopList = nearbyStopFinder.findNearbyStopsViaStreets(
      fromTarget ? verticesContainer.getToVertices() : verticesContainer.getFromVertices(),
      fromTarget,
      request,
      streetRequest
    );

    LOG.debug("Found {} {} stops", nearbyStopList.size(), fromTarget ? "egress" : "access");

    return nearbyStopList;
  }
}
