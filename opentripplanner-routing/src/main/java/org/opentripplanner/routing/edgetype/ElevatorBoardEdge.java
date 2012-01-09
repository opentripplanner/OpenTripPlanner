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

import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A relatively high cost edge for boarding an elevator.
 * @author mattwigway
 *
 */
public class ElevatorBoardEdge extends AbstractEdge {

    private static final long serialVersionUID = 3925814840369402222L;

    public ElevatorBoardEdge(Vertex from, Vertex to) {
        super(from, to);
    }
    
    @Override
    public State traverse(State s0) {
    	EdgeNarrative en = new FixedModeEdge(this, s0.getOptions().getModes().getNonTransitMode());
    	StateEditor s1 = s0.edit(this, en);
    	s1.incrementWeight(1);
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
        if (o instanceof ElevatorBoardEdge) {
            ElevatorBoardEdge other = (ElevatorBoardEdge) o;
            return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }
    
    public String toString() {
        return "ElevatorBoardEdge(" + fromv + " -> " + tov + ")";
    }
}
