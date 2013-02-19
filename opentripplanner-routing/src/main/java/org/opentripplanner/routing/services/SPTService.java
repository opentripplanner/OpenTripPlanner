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

package org.opentripplanner.routing.services;

import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.spt.ShortestPathTree;

public interface SPTService {
    
    /**
     * Generate a shortest path tree for this RoutingRequest.
     * 
     * @param req
     * @return
     */
    public ShortestPathTree getShortestPathTree(RoutingRequest req); 

    /**
     * Generate SPT, controlling the timeout externally.
     * 
     * @param req
     * @param timeoutSeconds
     * @return
     */
    public ShortestPathTree getShortestPathTree(RoutingRequest req, double timeoutSeconds);

    /**
     * Find a shortest path tree and control when the search terminates.
     * 
     * @param req
     * @param timeoutSeconds
     * @param terminationStrategy
     * @return
     */
    public ShortestPathTree getShortestPathTree(RoutingRequest req, double timeoutSeconds,
            SearchTerminationStrategy terminationStrategy);

}
