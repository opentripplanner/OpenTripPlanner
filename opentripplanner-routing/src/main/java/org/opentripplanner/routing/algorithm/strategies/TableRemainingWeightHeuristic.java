/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.algorithm.strategies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.algorithm.GraphLibrary;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RemainingWeightHeuristic for A* searches which makes use of a table of lower bounds on path costs between all pairs of stations.
 */
public class TableRemainingWeightHeuristic implements RemainingWeightHeuristic {
    private static final Logger LOG = LoggerFactory.getLogger(TableRemainingWeightHeuristic.class);

    // these variables remain unchanged after initialization
    private DefaultRemainingWeightHeuristic defaultHeuristic; // reuse perfectly good code (but is this slower?)

    private WeightTable wt;

    private Graph g;

    // these variables' values will be reused on subsequent calls
    // if the target vertex is the same
    private Vertex target;

    private List<NearbyStop> targetStops;

    // private HashSet<Vertex> targetStopSet; // was used when there were transfer links, which were too slow.
    private IdentityHashMap<Vertex, Double> weightCache;

    private TraverseOptions options;

    public TableRemainingWeightHeuristic(Graph g) {
        this.g = g;
        if (g.hasService(WeightTable.class)) {
            this.wt = g.getService(WeightTable.class);
            this.defaultHeuristic = new DefaultRemainingWeightHeuristic();
            LOG.debug("Created new table-driven heuristic object.");
        } else {
            throw new IllegalStateException(
                    "Graph must have weight table to use weight table heuristic.");
        }
    }

    /**
     * Use of this method is important to set up the table-driven heuristic instance. It needs to explore around the target vertex and find transit
     * stops in the vicinity. On subsequent calls, if the target is the same, this information will be reused.
     */
    @Override
    public double computeInitialWeight(State s0, Vertex target) {
        options = s0.getOptions();
        // do not check for identical options, since pathservice changes them from one call to the next
        if (target == this.target) {
            // no need to search again
            LOG.debug("Reusing target stop list.");
            return 0;
        }
        weightCache = new IdentityHashMap<Vertex, Double>(5000);
        this.target = target;
        targetStops = new ArrayList<NearbyStop>(50);
        Map<Vertex, List<Edge>> extraEdges = new HashMap<Vertex, List<Edge>>();
        // heap does not really need to be this big, verify initialization time
        ShortestPathTree spt = new BasicShortestPathTree(options);
        BinHeap<State> heap = new BinHeap<State>(100);
        State targetState = new State(target, s0.getTime(), s0.getOptions().reversedClone());
        spt.add(targetState);
        heap.insert(targetState, 0);
        while (!heap.empty()) {
            State u = heap.extract_min();
            if (!spt.visit(u))
                continue;

            // DEBUG since CH graphs are missing edges, and shortcuts have no walk distance
            // if (u.exceedsWeightLimit(60 * 15))
            // break;

            Vertex uVertex = u.getVertex();
            // LOG.debug("heap extract " + uVertex + " weight " + u.getWeight());
            weightCache.put(uVertex, u.getWeight());
            if (uVertex instanceof TransitStop) {
                targetStops.add(new NearbyStop(uVertex, u.getWalkDistance(), u.getWeight()));
                // LOG.debug("Target stop: " + uVertex + " w=" + u.getWeight());
                continue;
            }
            if (options.isArriveBy()) {
                for (Edge e : GraphLibrary.getOutgoingEdges(g, uVertex, extraEdges)) {
                    State v = e.traverse(u);
                    if (v != null && spt.add(v))
                        heap.insert(v, v.getWeight());
                }
            } else {
                for (Edge e : GraphLibrary.getIncomingEdges(g, uVertex, extraEdges)) {
                    State v = e.traverse(u);
                    if (v != null && spt.add(v))
                        heap.insert(v, v.getWeight());
                }
            }
        }
        LOG.debug("Found " + targetStops.size() + " stops near destination.");
        return defaultHeuristic.computeInitialWeight(s0, target);
    }

    @Override
    public double computeForwardWeight(State s0, Vertex target) {
        final double BOARD_COST = options.boardCost;
        Vertex v = s0.getVertex();
        // keep a cache (vertex->weight) here for multi-itinerary searches
        if (weightCache.containsKey(v))
            return weightCache.get(v);

        if (s0.getOptions().speed > wt.getMaxWalkSpeed()) {
            // fall back to slower heuristic if this heuristic would be inadmissible
            double w = defaultHeuristic.computeForwardWeight(s0, target);
            weightCache.put(s0.getVertex(), w);
            return w;
        }

        double w; // return value
        if (wt.includes(v)) {
            double remainingWalk = options.getMaxWalkDistance() - s0.getWalkDistance();
            w = Double.POSITIVE_INFINITY;
            for (NearbyStop ns : targetStops) {
                if (ns.distance > remainingWalk)
                    continue;
                double nw = wt.getWeight(v, ns.vertex) + ns.weight;
                if (nw < w)
                    w = nw;
            }
            if (!(v instanceof TransitStop))
                w -= BOARD_COST;
        } else {
            w = defaultHeuristic.computeForwardWeight(s0, target);
        }
        weightCache.put(v, w);
        return w;
    }

    @Override
    public double computeReverseWeight(State s0, Vertex target) {
        // TODO: Implement when computeForwardWeight is stable (AMB)
        return 0D;
    }

    // could just be a hashmap... but Entry<Vertex, Double> does not use primitive type
    private static class NearbyStop {
        protected double weight;

        protected double distance;

        protected Vertex vertex;

        protected NearbyStop(Vertex vertex, double distance, double weight) {
            this.vertex = vertex;
            this.distance = distance;
            this.weight = weight;
        }

        public String toString() {
            return "NearbyStop: " + vertex + " " + weight;
        }
    }

    @Override
    public void reset() {
        target = null;
    }

}
