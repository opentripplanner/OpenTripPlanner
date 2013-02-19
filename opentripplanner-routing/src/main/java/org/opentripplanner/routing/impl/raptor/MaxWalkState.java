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

package org.opentripplanner.routing.impl.raptor;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateData;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

public class MaxWalkState extends State {

    public MaxWalkState(Vertex v, RoutingRequest req) {
        super(v, req);
    }

    @Override
    public StateEditor edit(Edge e) {
        return new MaxWalkStateEditor(this, e);
    }

    @Override
    public boolean dominates(State other) {

        if (isBikeRenting() != other.isBikeRenting()) {
            return false;
        }

        if (backEdge != other.getBackEdge() && ((backEdge instanceof PlainStreetEdge)
                && (!((PlainStreetEdge) backEdge).getTurnRestrictions().isEmpty())))
            return false;
        return walkDistance <= other.getWalkDistance() * Raptor.WALK_EPSILON
                && this.getElapsedTime() <= other.getElapsedTime() + 30
                && getNumBoardings() <= other.getNumBoardings();
    }
    
    @Override
    public boolean exceedsWeightLimit(double maxWeight) {
        return false;
    }

    static class MaxWalkStateEditor extends StateEditor {

        public MaxWalkStateEditor(RoutingRequest options, Vertex v) {
            super();
            child = new MaxWalkState(v, options);
            child.stateData = new StateData(options);
        }

        @Override
        public boolean parsePath(State state) {
            return true;
        }
        
        public MaxWalkStateEditor(State parent, Edge e) {
            super(parent, e);
        }

        public boolean weHaveWalkedTooFar(RoutingRequest options) {
            // non-transit modes too
            return child.getWalkDistance() >= options.maxWalkDistance;
        }
    }
}
