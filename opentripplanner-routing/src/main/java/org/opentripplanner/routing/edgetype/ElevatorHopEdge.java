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

package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.graph.AbstractEdge;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A relatively low cost edge for travelling one level in an elevator.
 * @author mattwigway
 *
 */
public class ElevatorHopEdge extends AbstractEdge {

    private static final long serialVersionUID = 3925814840369402222L;

    private StreetTraversalPermission permission;

    public boolean wheelchairAccessible = true;

    public ElevatorHopEdge(Vertex from, Vertex to, StreetTraversalPermission permission) {
        super(from, to);
        this.permission = permission;
    }
    
    @Override
    public State traverse(State s0) {
        EdgeNarrative en = new FixedModeEdge(this, s0.getOptions().getModes().getNonTransitMode());
        TraverseOptions options = s0.getOptions();

        if (options.wheelchairAccessible && !wheelchairAccessible) {
            return null;
        }

        if (options.getModes().getWalk() && 
            !permission.allows(StreetTraversalPermission.PEDESTRIAN)) {
            return null;
        }

        if (options.getModes().getBicycle() && 
            !permission.allows(StreetTraversalPermission.BICYCLE)) {
            return null;
        }
        // there are elevators which allow cars
        if (options.getModes().getCar() && !permission.allows(StreetTraversalPermission.CAR)) {
            return null;
        }


        StateEditor s1 = s0.edit(this, en);
        s1.incrementWeight(options.elevatorHopCost);
        s1.incrementTimeInSeconds(options.elevatorHopTime);
        return s1.makeState();
    }

    @Override
    public double getDistance() {
        return 0;
    }

    @Override
    public Geometry getGeometry() {
        return null;
    }

    @Override
    public TraverseMode getMode() {
        return TraverseMode.WALK;
    }

    @Override
    public String getName() {
        return null;
    }
    
    public boolean equals(Object o) {
        if (o instanceof ElevatorHopEdge) {
            ElevatorHopEdge other = (ElevatorHopEdge) o;
            return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }
    
    public String toString() {
        return "ElevatorHopEdge(" + fromv + " -> " + tov + ")";
    }
}
