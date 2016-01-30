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

import com.google.common.collect.Lists;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is an adaptation of the bidiractional heuristic used in "long distance" mode, which was created after I
 * realized that heuristic is admissible but not monotonic. Being non-monotonic means nodes can be re-discovered and
 * paths are not discovered in order of increasing weight. That's problematic when finding paths one by one and
 * banning trips or routes -- suboptimal paths may be found and reported before or instead of optimal ones.
 *
 * So instead here we have an ultra-simplistic heuristic that maintains the same overall behavior as the bidirectional
 * heuristic but does not actually include any distance information. It only reports zero or infinity for for every
 * node.
 */
public class WalkConstrainingHeuristic implements RemainingWeightHeuristic {

    private static final long serialVersionUID = 20160126L;

    private static Logger LOG = LoggerFactory.getLogger(WalkConstrainingHeuristic.class);

    /** The vertex at which the main search begins. */
    Vertex origin;

    /** The vertex that the main search is working towards. */
    Vertex target;

    Set<Vertex> streetVerticesNearOrigin;

    Set<Vertex> streetVerticesNearDestination;

    Graph graph;

    RoutingRequest options;

    @Override
    public void initialize(RoutingRequest options, long abortTime) {
        Vertex target = options.rctx.target;
        if (target == this.target) {
            LOG.debug("Reusing existing heuristic, the target vertex has not changed.");
            return;
        }
        this.graph = options.rctx.graph;
        long start = System.currentTimeMillis();
        this.target = target;
        this.options = options;
        options.softWalkLimiting = false;
        options.softPreTransitLimiting = false;
        LOG.debug("initializing heuristic computation thread");
        // Forward street search first, mark street vertices around the origin so H evaluates to 0
        streetVerticesNearOrigin = streetSearch(options, false, abortTime);
        if (streetVerticesNearOrigin == null) {
            return; // Search timed out
        }
        LOG.debug("end forward street search {} ms", System.currentTimeMillis() - start);
        streetVerticesNearDestination = streetSearch(options, true, abortTime);
        if (streetVerticesNearDestination == null) {
            return; // Search timed out
        }
        LOG.debug("end backward street search {} ms", System.currentTimeMillis() - start);
        // once street searches are done, raise the limits to max
        // because hard walk limiting is incorrect and is observed to cause problems 
        // for trips near the cutoff
        options.setMaxWalkDistance(Double.POSITIVE_INFINITY);
        options.setMaxPreTransitTime(Integer.MAX_VALUE);
        LOG.debug("initialized SSSP");
        options.rctx.debugOutput.finishedPrecalculating();
    }

    /**
     * Always return zero or +INF!
     */
    @Override
    public double estimateRemainingWeight (State s) {
        final Vertex v = s.getVertex();
        // Temporary vertices (StreetLocations) might not be found in walk search.
        if (v instanceof StreetLocation) {
            return 0;
        }
        if (v instanceof StreetVertex) {
            // We're on the street, not on transit.
            if (s.isEverBoarded()) {
                // If we are not near the destination, do not pass into the street graph.
                if (streetVerticesNearDestination.contains(v)) {
                    return 0;
                } else {
                    return Double.POSITIVE_INFINITY;
                }
            } else {
                // We have not boarded transit yet.
                // We could also use the Euclidean heuristic here.
                if (streetVerticesNearOrigin.contains(v)) {
                    return 0;
                } else {
                    return Double.POSITIVE_INFINITY;
                }
            }
        } else {
            // We're not on a street, probably on transit. Explore this area of the graph fully.
            return 0;
        }
    }

    @Override
    public void reset() { }

    @Override
    public void doSomeWork() {
        // Do nothing.
    }

    private Set<Vertex> streetSearch (RoutingRequest rr, boolean fromTarget, long abortTime) {
        rr = rr.clone();
        if (fromTarget) {
            rr.setArriveBy(!rr.arriveBy);
        }
        Set<Vertex> vertices = new HashSet<>();
        ShortestPathTree spt = new DominanceFunction.MinimumWeight().getNewShortestPathTree(rr);
        // TODO use normal OTP search for this.
        BinHeap<State> pq = new BinHeap<State>();
        Vertex initVertex = fromTarget ? rr.rctx.target : rr.rctx.origin;
        State initState = new State(initVertex, rr);
        pq.insert(initState, 0);
        while ( ! pq.empty()) {
            if (abortTime < Long.MAX_VALUE  && System.currentTimeMillis() > abortTime) {
                return null;
            }
            State s = pq.extract_min();
            Vertex v = s.getVertex();
            if (! (v instanceof StreetVertex)) {
                continue;
            }
            // at this point the vertex is closed (pulled off heap).
            // on reverse search save measured weights.
            // the optimal path may use transit.
            // Without instanceOf check P+R and B+R doesn't work in depart by searches
            vertices.add(v);
            for (Edge e : rr.arriveBy ? v.getIncoming() : v.getOutgoing()) {
                // arriveBy has been set to match actual directional behavior in this subsearch
                State s1 = e.traverse(s);
                if (s1 == null)
                    continue;
                if (spt.add(s1)) {
                    pq.insert(s1,  s1.getWeight());
                }
            }
        }
        return vertices;
    }
 
}
