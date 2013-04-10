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

import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.common.pqueue.OTPPriorityQueue;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.EarliestArrivalShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Compute full SPT for earliest arrival problem. 
 * Always builds a full shortest path tree ("batch mode"). 
 * 
 * Note that walk limiting must be turned off -- resource limiting is not algorithmically correct.
 */
public class EarliestArrivalSPTService implements SPTService { 

    private static final Logger LOG = LoggerFactory.getLogger(EarliestArrivalSPTService.class);

    @Override
    public ShortestPathTree getShortestPathTree(RoutingRequest req) {
        return getShortestPathTree(req, -1, null); // negative timeout means no timeout
    }
    
    @Override
    public ShortestPathTree getShortestPathTree(RoutingRequest req, double timeoutSeconds) {
        return this.getShortestPathTree(req, timeoutSeconds, null);
    }

    public ShortestPathTree getShortestPathTree(RoutingRequest options, double relTimeout,
            SearchTerminationStrategy terminationStrategy) {
        
        // clone options before modifying, otherwise disabling resource limiting will cause 
        // SPT cache misses for subsequent requests.
        options = options.clone();
        
        // disable any resource limiting, which is algorithmically invalid here
        options.setMaxTransfers(Integer.MAX_VALUE);
        options.setMaxWalkDistance(Double.MAX_VALUE);
        if (options.getClampInitialWait() < 0)
            options.setClampInitialWait(60 * 30);
        
        // impose search cutoff
        final long maxt = (60 * 60 * 2) + options.getClampInitialWait();
        options.worstTime = options.dateTime + (options.arriveBy ? -maxt : maxt);
            
        // SPT cache does not look at routing request in SPT to perform lookup, 
        // so it's OK to construct with the local cloned one
        ShortestPathTree spt = new EarliestArrivalShortestPathTree(options); 
        State initialState = new State(options);
        spt.add(initialState);

        OTPPriorityQueue<State> pq = new BinHeap<State>();
        pq.insert(initialState, 0);

        while (!pq.empty()) {
            State u = pq.extract_min();
            Vertex u_vertex = u.getVertex();
            if (!spt.visit(u))
                continue;
            Collection<Edge> edges = options.isArriveBy() ? u_vertex.getIncoming() : u_vertex.getOutgoing();
            for (Edge edge : edges) {
                for (State v = edge.traverse(u); v != null; v = v.getNextResult()) {
                    if (isWorstTimeExceeded(v, options)) {
                        continue;
                    }
                    if (spt.add(v)) {
                        pq.insert(v, v.getActiveTime()); // activeTime?
                    } 
                }
            }
        }
        return spt;
    }

    // Move this into State
    private boolean isWorstTimeExceeded(State v, RoutingRequest opt) {
        if (opt.isArriveBy())
            return v.getTime() < opt.worstTime;
        else
            return v.getTime() > opt.worstTime;
    }

}
