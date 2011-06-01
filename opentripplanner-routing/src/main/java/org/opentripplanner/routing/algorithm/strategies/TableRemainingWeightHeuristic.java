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
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.algorithm.GraphLibrary;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.pqueue.BinHeap;
import org.opentripplanner.routing.spt.SPTVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RemainingWeightHeuristic for A* searches which makes use of a table of
 * lower bounds on path costs between all pairs of stations.
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
	
	public TableRemainingWeightHeuristic (Graph g) {
		this.g = g;
		if (g.hasService(WeightTable.class)) {
			this.wt = g.getService(WeightTable.class);
			this.defaultHeuristic = new DefaultRemainingWeightHeuristic();
			LOG.debug("Created new table-driven heuristic object.");
		} else {
			throw new IllegalStateException("Graph must have weight table to use weight table heuristic.");
		}
	}
	
	/**
	 * Use of this method is important to set up the table-driven heuristic instance. 
	 * It needs to explore around the target vertex and find transit stops in the vicinity.
	 * On subsequent calls, if the target is the same, this information will be reused.
	 */
	@Override
	public double computeInitialWeight(Vertex origin, Vertex target,
		TraverseOptions options) {
		// do not check for identical options, since pathservice changes them from one call to the next
		if (target == this.target) { 
			// no need to search again
    		LOG.debug("Reusing target stop list.");
			return 0;
		}
		weightCache = new IdentityHashMap<Vertex, Double>(200000); 
		this.target = target;
		targetStops = new ArrayList<NearbyStop>(50);
		Map<Vertex, List<Edge>> extraEdges = new HashMap<Vertex, List<Edge>>();
        options.extraEdgesStrategy.addIncomingEdgesForTarget(extraEdges, target);
	    final double MAX_WEIGHT = 60 * 15;
	    // heap does not really need to be this big, verify initialization time
	    BinHeap<Vertex> heap = new BinHeap<Vertex>(g.getVertices().size()); 
		HashSet<Vertex> closed = new HashSet<Vertex>();
    	heap.insert(target, 0);
    	while (! heap.empty()) {
    		double w = heap.peek_min_key();
    		Vertex u = heap.extract_min();
    		//LOG.debug("heap extract " + u + " weight " + w);
    		if (w > MAX_WEIGHT) break;
    		if (closed.contains(u)) continue; 
    		closed.add(u);
    		weightCache.put(u, w);
    		if (u instanceof TransitStop) {
    			targetStops.add(new NearbyStop(u, w));
    			//LOG.debug("Target stop: " + u + " w=" + w);
    		}
    		for (Edge e : GraphLibrary.getIncomingEdges(g, u, extraEdges)) {
    			if (e instanceof TurnEdge ||
					e instanceof PlainStreetEdge ||
					e instanceof StreetTransitLink ||
					e instanceof FreeEdge ) {
    				State s0 = new State();
    				TraverseResult tr = e.traverseBack(s0, options);
    				if (tr == null) continue;
    				Vertex fromv = tr.getEdgeNarrative().getFromVertex();
    				if (! closed.contains(fromv)) 
    					heap.insert(fromv, w + tr.weight);
    			}
    		}
    	}
		LOG.debug("Found " + targetStops.size() + " stops near destination.");
    	return defaultHeuristic.computeInitialWeight(origin, target, options);
    }

	@Override
	public double computeForwardWeight(SPTVertex from, Edge edge,
		TraverseResult traverseResult, Vertex target) {
	    final double BOARD_COST = 60 * 5;
		Vertex tov = traverseResult.getEdgeNarrative().getToVertex();
        // keep a cache (vertex->weight) here for multi-itinerary searches
        if (weightCache.containsKey(tov)) return weightCache.get(tov);
        double w; // return value
        if (wt.includes(tov)) {
			w = Double.POSITIVE_INFINITY;
			for (NearbyStop ns : targetStops) {
				double nw = wt.getWeight(tov, ns.vertex) + ns.weight;
				if (nw < w) w = nw;
			}	
			if (! (tov instanceof TransitStop)) w -= BOARD_COST;
		} else {
			w = defaultHeuristic.computeForwardWeight(from, edge, traverseResult, target); 
		}
        weightCache.put(tov, w);
        return w;
	}

	@Override
	public double computeReverseWeight(SPTVertex from, Edge edge, 
			TraverseResult traverseResult, Vertex target) {
		// will be implemented when computeForwardWeight is stable
		return 0D;
	}
	
	// could just be a hashmap... but Entry<Vertex, Double> does not use primitive type
	private static class NearbyStop {
        protected double weight;
        protected Vertex vertex;
        protected NearbyStop (Vertex vertex, double weight) {
            this.vertex = vertex;
            this.weight = weight;
        }
        public String toString() {
            return "NearbyStop: " + vertex + " " + weight;
        }
    }
	
}
