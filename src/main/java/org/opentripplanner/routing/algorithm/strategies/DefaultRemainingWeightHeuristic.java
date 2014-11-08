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

import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.collect.Maps;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.profile.StopAtDistance;
import org.opentripplanner.profile.StopCluster;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * A Euclidean remaining weight strategy that takes into account transit boarding costs where applicable.
 * 
 */
public class DefaultRemainingWeightHeuristic implements RemainingWeightHeuristic {

    private static final long serialVersionUID = -5172878150967231550L;
    private static Logger LOG = LoggerFactory.getLogger(DefaultRemainingWeightHeuristic.class);

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();
    private double targetX;
    private double targetY;
    private boolean transit;
    private double walkReluctance;
    private double maxStreetSpeed;
    private double maxTransitSpeed;
    private double requiredWalkDistance;

    @Override
    public void initialize(RoutingRequest options, Vertex origin, Vertex target, long abortTime) {
        RoutingRequest req = options;
        this.transit = req.modes.isTransit();
        maxStreetSpeed = req.getStreetSpeedUpperBound();
        maxTransitSpeed = req.getTransitSpeedUpperBound();
        targetX = target.getX();
        targetY = target.getY();
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
    public double computeForwardWeight(State s, Vertex target) {
        Vertex sv = s.getVertex();
        double euclideanDistance = distanceLibrary.fastDistance(sv.getY(), sv.getX(), targetY, targetX);
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
    private double determineRequiredWalkDistance(RoutingRequest req) {
        if ( ! req.modes.isTransit()) return 0; // required walk distance will be unused.
        GenericDijkstra gd = new GenericDijkstra(req);
        State s = new State(req.rctx.target, req);
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

    static class ClosestStopTraverseVisitor implements TraverseVisitor {
        public double distanceToClosestStop = Double.POSITIVE_INFINITY;
        @Override public void visitEdge(Edge edge, State state) { }
        @Override public void visitEnqueue(State state) { }
        @Override public void visitVertex(State state) {
            if (state.getVertex() instanceof TransitStop) {
                distanceToClosestStop = state.getWalkDistance();
                LOG.info("Found closest stop to search target: {} at {}m", state.getVertex(), (int) distanceToClosestStop);
            }
        }
    }

    /**
     * computeForwardWeight and computeReverseWeight were identical (except that 
     * computeReverseWeight did not have the localStreetService clause). They have been merged.
     */
    @Override
    public double computeReverseWeight(State s, Vertex target) {
        return computeForwardWeight(s, target);
    }

    @Override
    public void reset() {}

    @Override
    public void doSomeWork() {}

}
