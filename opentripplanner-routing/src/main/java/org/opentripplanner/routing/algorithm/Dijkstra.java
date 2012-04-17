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
import java.util.Set;

import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.contraction.ContractionHierarchy;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.BasicShortestPathTree;

/**
 * Find the shortest path between graph vertices using Dijkstra's algorithm. 
 */
public class Dijkstra {

    Vertex taboo;
    private BasicShortestPathTree spt;
    private BinHeap<State> queue;
    private HashSet<Vertex> targets = null; 
    private int hopLimit;
    Set<Vertex> routeOn;
    
    public Dijkstra(Vertex origin, TraverseOptions options, Vertex taboo) {
        this(origin, options, taboo, Integer.MAX_VALUE);
    }
    /**
     * 
     * @param taboo Do not consider any paths passing through this vertex
     */
    public Dijkstra(Vertex origin, TraverseOptions options, Vertex taboo, int hopLimit) {
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
        spt = new BasicShortestPathTree(options);
        queue = new BinHeap<State>(50);
        // Never init time to 0 since traverseBack will give times less than 0
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

            Iterable<Edge> outgoing = u.getOutgoing();
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
        
        // clone targets since they will be checked off destructively
    	HashSet<Vertex> remainingTargets = null;
    	if (targets != null)
    		remainingTargets = (HashSet<Vertex>) targets.clone();

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
            
            Iterable<Edge> outgoing = u.getOutgoing();
            for (Edge edge : outgoing) {

                if (routeOn != null && ! routeOn.contains(edge.getToVertex()))
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
    
    public void setRouteOn(Set<Vertex> routeOn) {
        this.routeOn = routeOn;
    }

    public void setTargets(Collection<State> targets) {
        this.targets = new HashSet<Vertex>(targets.size());
        for (State s : targets) {
            this.targets.add(s.getVertex());
        }
    }

}
