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

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;

/**
 * A trivial heuristic that always returns 0, which is always admissible. 
 * For use in testing and troubleshooting.
 * 
 * @author andrewbyrd
 */
public class TrivialRemainingWeightHeuristic implements RemainingWeightHeuristic {

    @Override
    public double computeInitialWeight(State s, Vertex target) {
        return 0;
    }

    @Override
    public double computeForwardWeight(State s, Vertex target) {
        return 0;
    }

    @Override
    public double computeReverseWeight(State s, Vertex target) {
        return 0;
    }

    @Override
    public void reset() {}

    /** 
     * Factory that turns off goal-direction heuristics in OTP for comparison. 
     * results should be identical when heuristics are switched off.
     */
    public static class Factory implements RemainingWeightHeuristicFactory {
        @Override
        public RemainingWeightHeuristic getInstanceForSearch(RoutingRequest opt) {
            return new TrivialRemainingWeightHeuristic();
        }
    }
}
