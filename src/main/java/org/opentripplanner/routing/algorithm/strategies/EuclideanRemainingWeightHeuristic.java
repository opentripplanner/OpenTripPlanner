package org.opentripplanner.routing.algorithm.strategies;

import com.google.common.collect.Iterables;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

/**
 * A Euclidean remaining weight strategy that takes into account transit boarding costs where applicable.
 * 
 */
public class EuclideanRemainingWeightHeuristic implements RemainingWeightHeuristic {

    private static final long serialVersionUID = -5172878150967231550L;

    private double lat;
    private double lon;
    private double maxStreetSpeed;

    @Override
    public void initialize(RoutingRequest options, long abortTime) {
        RoutingRequest req = options;
        Vertex target = req.rctx.target;
        maxStreetSpeed = req.getStreetSpeedUpperBound();

        if (target.getDegreeIn() == 1) {
            Edge edge = Iterables.getOnlyElement(target.getIncoming());
            if (edge instanceof FreeEdge) {
                target = edge.getFromVertex();
            }
        }

        lat = target.getLat();
        lon = target.getLon();
    }

    /**
     * On a non-transit trip, the remaining weight is simply distance / street speed.
     */
    @Override
    public double estimateRemainingWeight (State s) {
        Vertex sv = s.getVertex();
        double euclideanDistance = SphericalDistanceLibrary.fastDistance(sv.getLat(), sv.getLon(), lat, lon);
        // all travel is on-street, no transit involved
        return euclideanDistance / maxStreetSpeed;
    }

    @Override
    public void reset() {}

    @Override
    public void doSomeWork() {}

}
