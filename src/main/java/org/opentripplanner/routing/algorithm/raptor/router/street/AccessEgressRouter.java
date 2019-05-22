package org.opentripplanner.routing.algorithm.raptor.router.street;

import org.opentripplanner.graph_builder.module.NearbyStopFinder;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class AccessEgressRouter {
    private static Logger LOG = LoggerFactory.getLogger(AccessEgressRouter.class);

    private AccessEgressRouter() {}

    public static Map<Stop, Transfer> streetSearch (RoutingRequest rr, boolean fromTarget, int distanceMeters) {
        Vertex vertex = fromTarget ? rr.rctx.toVertex : rr.rctx.fromVertex;

        NearbyStopFinder nearbyStopFinder = new NearbyStopFinder(rr.rctx.graph, distanceMeters, true);
        List<NearbyStopFinder.StopAtDistance> stopAtDistanceList =
                nearbyStopFinder.findNearbyStopsViaStreets(vertex, fromTarget);

        Map<Stop, Transfer> result = new HashMap<>();
        for (NearbyStopFinder.StopAtDistance stopAtDistance : stopAtDistanceList) {
            result.put(
                    stopAtDistance.tstop.getStop(),
                    new Transfer(-1,
                            (int)stopAtDistance.edges.stream().map(Edge::getDistance)
                                    .collect(Collectors.summarizingDouble(Double::doubleValue)).getSum(),
                            Arrays.asList(stopAtDistance.geom.getCoordinates())));
        }

        LOG.info("Found {} {} stops", result.size(), fromTarget ? "egress" : "access");

        return result;
    }
}
