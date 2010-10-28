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

package org.opentripplanner.routing.edgetype.factory;

import java.util.HashSet;

import org.opentripplanner.routing.algorithm.NegativeWeightException;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.pqueue.FibHeap;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.SPTVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FindMaxWalkDistances {
    
    private final static Logger _log = LoggerFactory.getLogger(FindMaxWalkDistances.class);

    public static void find(Graph graph) {
        _log.debug("finding max walk distances");
        for (GraphVertex gv : graph.getVertices()) {
            if (gv.vertex instanceof TransitStop) { 
                assignStopDistances(graph, (TransitStop) gv.vertex);
            }
        }
    }
    
    private static void assignStopDistances(Graph graph, TransitStop origin) {
        
        TraverseOptions options = new TraverseOptions(new TraverseModeSet(TraverseMode.WALK));
        options.maxWalkDistance = Double.MAX_VALUE;
        options.walkReluctance = 1.0;
        options.speed = 1.0;
        
        // Iteration Variables
        SPTVertex spt_u, spt_v;
        HashSet<Vertex> closed = new HashSet<Vertex>();
        FibHeap<SPTVertex> queue = new FibHeap<SPTVertex>(graph.getVertices().size());
        BasicShortestPathTree spt = new BasicShortestPathTree();
        State init = new State();
        SPTVertex spt_origin = spt.addVertex(origin, init, 0, options);
        queue.insert(spt_origin, spt_origin.weightSum);

        while (!queue.empty()) { // Until the priority queue is empty:

            spt_u = queue.peek_min(); // get the lowest-weightSum Vertex 'u',

            Vertex fromv = spt_u.mirror;

            queue.extract_min();

            closed.add(fromv);

            Iterable<Edge> outgoing = graph.getOutgoing(fromv);
            State state = spt_u.state;

            for (Edge edge : outgoing) {
                Vertex toVertex = edge.getToVertex();

                if (closed.contains(toVertex)) {
                    continue;
                }

                TraverseResult wr = edge.traverse(state, options);

                // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                // over.
                if (wr == null) {
                    continue;
                }

                if (wr.weight < 0) {
                    throw new NegativeWeightException(String.valueOf(wr.weight));
                }

                double new_w = spt_u.weightSum + wr.weight;

                if (toVertex instanceof StreetVertex) {
                    StreetVertex sv = (StreetVertex) toVertex;
                    if (sv.getDistanceToNearestTransitStop() <= new_w) {
                        continue;
                    }
                    sv.setDistanceToNearestTransitStop(new_w);
                }
                
                spt_v = spt.addVertex(toVertex, wr.state, new_w, options, spt_u.hops + 1);

                if (spt_v != null) {
                    spt_v.setParent(spt_u, edge);
                    queue.insert_or_dec_key(spt_v, new_w);
                }
            }
        }
    }

}
