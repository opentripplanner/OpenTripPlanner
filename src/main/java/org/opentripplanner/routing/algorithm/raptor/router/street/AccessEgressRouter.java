package org.opentripplanner.routing.algorithm.raptor.router.street;

import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     * @return Transfer objects by access/egress stop
     */
    public static Map<Stop, Transfer> streetSearch (RoutingRequest rr, boolean fromTarget) {
        int desiredDistance = fromTarget ? rr.desiredMaxWalkDistanceAccess : rr.desiredMaxWalkDistanceEgress;
        int maxDistance = fromTarget ? rr.maxWalkDistanceAccess : rr.maxWalkDistanceEgress;


        // Make sure that always one try is made.
        if (desiredDistance > maxDistance) {
            desiredDistance = maxDistance;
        }

        Map<Stop, Transfer> result = null;
        while (desiredDistance <= maxDistance) {
            result = streetSearch(rr, fromTarget, desiredDistance);
            if (result.size() > 0) {
                return result;
            }
            desiredDistance += 1000;

            if (desiredDistance >= maxDistance) {
                result = streetSearch(rr, fromTarget, maxDistance);
                return result;
            }
        }

        return result;
    }

    /**
     *
     * @param rr the current routing request
     * @param fromTarget whether to route from or towards the point provided in the routing request
     *                   (access or egress)
     * @param distanceMeters the maximum street distance to search for access/egress stops
     * @return Transfer objects by access/egress stop
     */
    public static Map<Stop, Transfer> streetSearch (RoutingRequest rr, boolean fromTarget, int distanceMeters) {
        Vertex vertex = fromTarget ? rr.rctx.toVertex : rr.rctx.fromVertex;

        NearbyStopFinder nearbyStopFinder = new NearbyStopFinder(rr.rctx.graph, distanceMeters, true);
        // We set removeTempEdges to false because this is a sub-request - the temporary edges for the origin and
        // target vertex will be cleaned up at the end of the super-request, and we don't want that to happen twice.
        List<NearbyStopFinder.StopAtDistance> stopAtDistanceList =
                nearbyStopFinder.findNearbyStopsViaStreets(vertex, fromTarget, false);

        Map<Stop, Transfer> result = new HashMap<>();
        for (NearbyStopFinder.StopAtDistance stopAtDistance : stopAtDistanceList) {
            result.put(
                    stopAtDistance.tstop.getStop(),
                    new Transfer(-1,
                            (int)stopAtDistance.edges.stream().map(Edge::getEffectiveWalkDistance)
                                    .collect(Collectors.summarizingDouble(Double::doubleValue)).getSum(),
                            stopAtDistance.edges));
        }

        LOG.info("Found {} {} stops", result.size(), fromTarget ? "egress" : "access");

        return result;
    }
}
