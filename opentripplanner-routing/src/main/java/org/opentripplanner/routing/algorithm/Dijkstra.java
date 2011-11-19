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

package org.opentripplanner.routing.algorithm;

import java.util.Collection;
import java.util.HashSet;

import org.opentripplanner.routing.contraction.ContractionHierarchy;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.OverlayGraph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.pqueue.BinHeap;
import org.opentripplanner.routing.spt.BasicShortestPathTree;

/**
 * Find the shortest path between graph vertices using Dijkstra's algorithm. 
 */
public class Dijkstra {

    Vertex taboo;
    private BasicShortestPathTree spt;
    private BinHeap<State> queue;
    private HashSet<Vertex> targets = null; // why was this a set of String not Vertex?
    
    private int hopLimit;
    private OverlayGraph graph;
    
    public Dijkstra(OverlayGraph graph, Vertex origin, TraverseOptions options, Vertex taboo) {
        this(graph, origin, options, taboo, Integer.MAX_VALUE);
    }
    /**
     * 
     * @param taboo Do not consider any paths passing through this vertex
     */
    public Dijkstra(OverlayGraph graph, Vertex origin, TraverseOptions options, Vertex taboo, int hopLimit) {
        this.graph = graph;
        this.taboo = taboo;
        this.hopLimit = hopLimit;

        /* Knowing that these are hop- or weight-limited searches called
         * in a tight loop, it is important not to set the initial size 
         * too large, otherwise a ton of time is wasted at each iteration
         * initializing unused queue/heap elements. This makes an immense 
         * difference in contraction hierarchy construction time, more than 
         * any other change or optimization I have tried. (AMB) 
         * 
         * A good guess would be: ceiling(graph.averageDegree) ** hoplimit
         */
        spt = new BasicShortestPathTree(50);
        queue = new BinHeap<State>(50);
        // Never init time to 0 since traverseBack will give times less than 0
        // which in certain cases can trigger
        State init = new State(origin, options);
        spt.add(init);
        queue.insert(init, init.getWeight());
    }
    
    /**
     * Plots a path on graph from origin to target, departing at the time 
     * given in state and with the options options.  
     * 
     * @param graph
     * @param origin
     * @param target
     * @return the shortest path, or null if none is found
     */

    public BasicShortestPathTree getShortestPathTree(Vertex target, double weightLimit) {
        
        while (!queue.empty()) { 
            State su = queue.extract_min(); 
            if ( ! spt.visit(su)) 
            	continue;
            
            if (su.exceedsWeightLimit(weightLimit))
                return spt;

            Vertex u = su.getVertex();
            if (u == target)
                break;

            Iterable<Edge> outgoing = graph.getOutgoing(u);
            for (Edge edge : outgoing) {
            	State sv = edge.traverse(su);
                if (sv != null
                   	&& sv.getVertex() != taboo
                   	&& ! sv.exceedsHopLimit(hopLimit)
                	&& spt.add(sv))
                        queue.insert(sv, sv.getWeight());
            }
        }
        return spt;
    }
    

    
    @SuppressWarnings("unchecked")
	public BasicShortestPathTree getShortestPathTree(double weightLimit, int nodeLimit) {
        
//        if (targets != null) {
//            targets.remove(origin);
//        }
    	// clone targets since they will be checked off destructively
    	HashSet<String> remainingTargets = null;
    	if (targets != null)
    		remainingTargets = (HashSet<String>) targets.clone();

        while (!queue.empty()) {
            
            State su = queue.extract_min();

            if ( ! spt.visit(su)) 
            	continue;

            Vertex u = su.getVertex();
            
            if (nodeLimit-- <= 0) 
            	break;
            
            if (remainingTargets != null) {
                remainingTargets.remove(u);
                if (remainingTargets.isEmpty())
                	break;
            }
            
            Iterable<Edge> outgoing = graph.getOutgoing(u);
            for (Edge edge : outgoing) {
//                if (!(edge instanceof TurnEdge || edge instanceof FreeEdge || edge instanceof Shortcut || edge instanceof PlainStreetEdge)) {
//                    //only consider street edges when contracting
//                    // use isContractable() ?
//                    continue;
//                }
            	if ( ! (ContractionHierarchy.isContractable(edge)))
            		continue;
            	
                State sv = edge.traverse(su);
                
                if (sv != null
                	&& sv.getVertex() != taboo
                	&& spt.add(sv)
                	&& !sv.exceedsHopLimit(hopLimit) 
                	&& !sv.exceedsWeightLimit(weightLimit))
                        queue.insert(sv, sv.getWeight());
            }
        }
        return spt;
    }

    public void setTargets(HashSet<Vertex> targets) {
        this.targets = targets;
    }
    
    public void setTargets(Collection<State> targets) {
        this.targets = new HashSet<Vertex>(targets.size());
        for (State s : targets) {
            this.targets.add(s.getVertex());
        }
    }

}
