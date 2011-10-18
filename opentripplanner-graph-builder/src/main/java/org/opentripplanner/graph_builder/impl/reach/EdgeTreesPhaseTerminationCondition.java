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

package org.opentripplanner.graph_builder.impl.reach;

import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

public class EdgeTreesPhaseTerminationCondition implements SearchTerminationStrategy {

    private int vertexLimit;

    private boolean unsafe = false;
    
    public EdgeTreesPhaseTerminationCondition(int vertexLimit) {
        this.vertexLimit = vertexLimit;
    }

    @Override
    public boolean shouldSearchContinue(Vertex origin, Vertex target, State current,
            ShortestPathTree spt, TraverseOptions traverseOptions) {
        if (current.getVertex() instanceof TransitStop) {
            unsafe = true;
            return false;
        }
        unsafe = spt.getVertexCount() >= vertexLimit;
        return !unsafe;
    }
 
    public boolean getUnsafeTermination() {
        return unsafe;
    }
    
}
