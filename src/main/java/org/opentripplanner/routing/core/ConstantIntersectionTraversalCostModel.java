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

package org.opentripplanner.routing.core;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

/**
 * The cost of traversing an intersection is constant.
 * @author avi
 */
public class ConstantIntersectionTraversalCostModel extends AbstractIntersectionTraversalCostModel {
    
    private double cost;
    
    /**
     * All traversal costs are equal to the passed-in constant.
     */
    public ConstantIntersectionTraversalCostModel(double cost) {
        this.cost = cost;
    }
    
    /**
     * Convenience constructor for no cost.
     */
    public ConstantIntersectionTraversalCostModel() {
        this(0.0);
    }
    
    @Override
    public double computeTraversalCost(IntersectionVertex v, StreetEdge from, StreetEdge to, TraverseMode mode,
            RoutingRequest options, float fromSpeed, float toSpeed) {
        return cost;
    }

}
