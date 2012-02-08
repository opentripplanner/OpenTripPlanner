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

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Interface for classes that provides an admissible estimate of (lower bound on) 
 * the weight of a path to the target, starting from a given state.
 */
public interface RemainingWeightHeuristic extends Serializable {
	
	/** 
	 * It is important to evaluate the initial weight before computing additional weights, 
	 * because this method also performs any one-time setup and precomputation that will be used
	 * by the heuristic during the search. 
	 */
    public double computeInitialWeight(State s, Vertex target);

    public double computeForwardWeight(State s, Vertex target);

    public double computeReverseWeight(State s, Vertex target);
    
    /** 
     * Reset any cached data in the heuristic 
     */
    public void reset();
}
