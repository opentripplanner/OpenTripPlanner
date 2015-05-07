/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.algorithm.strategies;

import com.google.common.collect.Iterables;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Euclidean remaining weight strategy that takes into account transit boarding costs where applicable.
 * 
 */
public class EuclideanRemainingWeightHeuristic implements RemainingWeightHeuristic {

    private static final long serialVersionUID = -5172878150967231550L;
    private static Logger LOG = LoggerFactory.getLogger(EuclideanRemainingWeightHeuristic.class);

    private double lat;
    private double lon;
    private boolean transit;
    private double walkReluctance;
    private double maxStreetSpeed;
    private double maxTransitSpeed;
    private double requiredWalkDistance;

    @Override
    public void initialize(RoutingRequest options, long abortTime) {
        RoutingRequest req = options;
        Vertex target = req.rctx.target;
        this.transit = req.modes.isTransit();
        maxStreetSpeed = req.getStreetSpeedUpperBound();
        maxTransitSpeed = req.getTransitSpeedUpperBound();

        if (target.getDegreeIn() == 1) {
            Edge edge = Iterables.getOnlyElement(target.getIncoming());
            if (edge instanceof FreeEdge) {
                target = edge.getFromVertex();
            }
        }

        lat = target.getLat();
        lon = target.getLon();
        requiredWalkDistance = determineRequiredWalkDistance(req);
        walkReluctance = req.walkReluctance;
    }

    /**
     * On a non-transit trip, the remaining weight is simply distance / street speed.
     * On a transit trip, there are two cases: 
     * (1) we're not on a transit vehicle. In this case, there are two possible ways to compute 
     *     the remaining distance, and we take whichever is smaller: 
     *     (a) walking distance / walking speed 
     *     (b) boarding cost + transit distance / transit speed (this is complicated a bit when 
     *         we know that there is some walking portion of the trip). 
     * (2) we are on a transit vehicle, in which case the remaining weight is simply transit 
     *     distance / transit speed (no need for boarding cost), again considering any mandatory 
     *     walking.
     */
    @Override
    public double estimateRemainingWeight (State s) {
        Vertex sv = s.getVertex();
        double euclideanDistance = SphericalDistanceLibrary.fastDistance(sv.getLat(), sv.getLon(), lat, lon);
        if (transit) {
            if (euclideanDistance < requiredWalkDistance) {
                return walkReluctance * euclideanDistance / maxStreetSpeed;
            }
            /* Due to the above conditional, the following value is known to be positive. */
            double transitWeight = (euclideanDistance - requiredWalkDistance) / maxTransitSpeed;
            double streetWeight = walkReluctance * (requiredWalkDistance / maxStreetSpeed);
            return transitWeight + streetWeight;
        } else {
            // all travel is on-street, no transit involved
            return walkReluctance * euclideanDistance / maxStreetSpeed;
        }
    }

    /**
     * Figure out the minimum amount of walking to reach the destination from transit.
     * This is done by doing a Dijkstra search for the first reachable transit stop.
     */
    private double determineRequiredWalkDistance(final RoutingRequest req) {
        if (!transit) return 0; // required walk distance will be unused.
        RoutingRequest options = req.clone();
        options.setArriveBy(!req.arriveBy);
        options.setRoutingContext(req.rctx.graph, req.rctx.fromVertex, req.rctx.toVertex);
        GenericDijkstra gd = new GenericDijkstra(options);
        State s = new State(options);
        gd.setHeuristic(new TrivialRemainingWeightHeuristic());
        final ClosestStopTraverseVisitor visitor = new ClosestStopTraverseVisitor();
        gd.traverseVisitor = visitor;
        gd.searchTerminationStrategy = new SearchTerminationStrategy() {
            @Override public boolean shouldSearchTerminate(Vertex origin, Vertex target, State current,
                                                           ShortestPathTree spt, RoutingRequest traverseOptions) {
                return visitor.distanceToClosestStop != Double.POSITIVE_INFINITY;
            }
        };
        gd.getShortestPathTree(s);
        return visitor.distanceToClosestStop;
    }

    private class ClosestStopTraverseVisitor implements TraverseVisitor {
        private double distanceToClosestStop = Double.POSITIVE_INFINITY;

        @Override public void visitEdge(Edge edge, State state) { }
        @Override public void visitEnqueue(State state) { }
        @Override public void visitVertex(State state) {
            Edge backEdge = state.getBackEdge();

            if (backEdge instanceof StreetTransitLink) {
                Vertex backVertex = state.getBackState().getVertex();
                distanceToClosestStop = SphericalDistanceLibrary.fastDistance(
                        backVertex.getLat(), backVertex.getLon(), lat, lon);
                LOG.debug("Found closest stop to search target: {} at {}m",
                        state.getVertex(), (int) distanceToClosestStop);
            } else if (state.getVertex() instanceof TransitStationStop && backEdge == null) {
                LOG.debug("Search target is a transit stop, no walking is required at end of trip");
                distanceToClosestStop = 0;
            }
        }
    }

    @Override
    public void reset() {}

    @Override
    public void doSomeWork() {}

}
