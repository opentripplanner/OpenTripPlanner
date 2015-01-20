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

import java.io.Serializable;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Interface for classes that provides an admissible estimate of (lower bound on) 
 * the weight of a path to the target, starting from a given state.
 */
public interface RemainingWeightHeuristic extends Serializable {
	
    /** 
     * Perform any one-time setup and pre-computation that will be needed by later calls to 
     * computeForwardWeight/computeReverseWeight. 
     */
    public void initialize(RoutingRequest options, Vertex origin, Vertex target, long abortTime);

    public double computeForwardWeight(State s, Vertex target);

    public double computeReverseWeight(State s, Vertex target);
    
    /** Reset any cached data in the heuristic, e.g. between rounds of a retrying path service. */
    public void reset();
    
    /** 
     * Call to cause the heuristic to perform some predetermined amount of work improving its 
     * estimate. Avoids thread synchronization evil by interleaving forward and backward searches. 
     */
    public void doSomeWork();
    
}


// Perhaps directionality should also be defined during the setup,
// instead of having two separate methods for the two directions.
// We might not even need a setup method if the routing options are just passed into the
// constructor.
