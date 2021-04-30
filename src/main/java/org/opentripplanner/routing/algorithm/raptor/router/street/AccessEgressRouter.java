package org.opentripplanner.routing.algorithm.raptor.router.street;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This uses a street search to find paths to all the access/egress stop within range
 */
public class AccessEgressRouter {
    private static Logger LOG = LoggerFactory.getLogger(AccessEgressRouter.class);

    private AccessEgressRouter() {}

    /**
     *
     * @param rr the current routing request
     * @param fromTarget whether to route from or towards the point provided in the routing request
     *                   (access or egress)
     * @param distanceMeters the maximum street distance to search for access/egress stops
     * @return Transfer objects by access/egress stop
     */
    public static Collection<NearbyStop> streetSearch (
        RoutingRequest rr,
        StreetMode streetMode,
        boolean fromTarget,
        int distanceMeters
    ) {
        // TODO OTP2 This has to be done because we have not separated the main RoutingRequest from
        //      the subrequest for street searches. From/to vertices are already set based on the main
        //      request being arriveBy or not, but here we are actually setting arriveBy based on
        //      whether we are doing an access or egress search, regardless of the direction of the
        //      main request.
        Set<Vertex> vertices = fromTarget ^ rr.arriveBy ? rr.rctx.toVertices : rr.rctx.fromVertices;

        // A new RoutingRequest needs to be constructed, because findNearbyStopsViaStreets() resets
        // the routingContext (rctx), which results in the created temporary edges being removed prematurely.
        // findNearbyStopsViaStreets() will call cleanup() on the created routing request.
        RoutingRequest nearbyRequest = rr.getStreetSearchRequest(streetMode);

        NearbyStopFinder nearbyStopFinder = new NearbyStopFinder(rr.rctx.graph, distanceMeters, true);
        List<NearbyStop> nearbyStopList = nearbyStopFinder.findNearbyStopsViaStreets(
            vertices,
            fromTarget,
            true,
            nearbyRequest
        );

        LOG.debug("Found {} {} stops", nearbyStopList.size(), fromTarget ? "egress" : "access");

        return nearbyStopList;
    }
}
