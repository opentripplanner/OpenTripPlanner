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
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;

public class EdgeTreesPhaseTerminationCondition implements SearchTerminationStrategy {

    private int vertexLimit;

    private boolean unsafe = false;
    
    public EdgeTreesPhaseTerminationCondition(int vertexLimit) {
        this.vertexLimit = vertexLimit;
    }

    @Override
    public boolean shouldSearchContinue(Vertex origin, Vertex target, State current,
            ShortestPathTree spt, RoutingRequest traverseOptions) {
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
