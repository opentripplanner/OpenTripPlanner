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

/**
 * A trivial heuristic that always returns 0, which is always admissible. For use in testing, troubleshooting, and
 * spatial analysis applications where there is no target.
 */
public class TrivialRemainingWeightHeuristic implements RemainingWeightHeuristic {

    private static final long serialVersionUID = 1L;

    @Override
    public void initialize(RoutingRequest options, long abortTime) {}

    @Override
    public double estimateRemainingWeight (State s) {
        return 0;
    }

    @Override
    public void reset() {}
    
    @Override
    public void doSomeWork() {}

}
