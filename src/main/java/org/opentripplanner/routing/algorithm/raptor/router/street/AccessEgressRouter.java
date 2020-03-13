package org.opentripplanner.routing.algorithm.raptor.router.street;

import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.request.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public static Map<Stop, Transfer> streetSearch (RoutingRequest rr, boolean fromTarget, int distanceMeters) {
        Set<Vertex> vertices = fromTarget ? rr.rctx.toVertices : rr.rctx.fromVertices;

        NearbyStopFinder nearbyStopFinder = new NearbyStopFinder(rr.rctx.graph, distanceMeters, true);
        // We set removeTempEdges to false because this is a sub-request - the temporary edges for the origin and
        // target vertex will be cleaned up at the end of the super-request, and we don't want that to happen twice.
        List<NearbyStopFinder.StopAtDistance> stopAtDistanceList =
                nearbyStopFinder.findNearbyStopsViaStreets(vertices, fromTarget, false);

        Map<Stop, Transfer> result = new HashMap<>();
        for (NearbyStopFinder.StopAtDistance stopAtDistance : stopAtDistanceList) {
            result.put(
                    stopAtDistance.tstop.getStop(),
                    new Transfer(-1,
                            (int)stopAtDistance.edges.stream().map(Edge::getEffectiveWalkDistance)
                                    .collect(Collectors.summarizingDouble(Double::doubleValue)).getSum(),
                            stopAtDistance.edges));
        }

        LOG.debug("Found {} {} stops", result.size(), fromTarget ? "egress" : "access");

        return result;
    }
}
