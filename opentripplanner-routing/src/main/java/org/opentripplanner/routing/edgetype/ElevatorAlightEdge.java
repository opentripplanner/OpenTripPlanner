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
 * A relatively low cost edge for alighting from an elevator.
 * All narrative generation is done by the ElevatorAlightEdge (other edges are silent), because
 * it is the only edge that knows where the user is to get off.
 * @author mattwigway
 *
 */
public class ElevatorAlightEdge extends AbstractEdge implements EdgeNarrative {

    private static final long serialVersionUID = 3925814840369402222L;

    /**
     * This is the level of this elevator exit, used in narrative generation.
     */
    private Float level;

    /**
     * @param level It's a float for future expansion.
     */
    public ElevatorAlightEdge(Vertex from, Vertex to, Float level) {
        super(from, to);
	this.level = level;
    }
    
    @Override
    public State traverse(State s0) {
	// we are our own edge narrative
    	StateEditor s1 = s0.edit(this, this);
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
        return TraverseMode.ELEVATOR;
    }

    /** 
     * The level from OSM is the name
     */
    @Override
    public String getName() {
        return level.toString();
    }

    /**
     * The name is not bogus; it's level n from OSM.
     * @author mattwigway
     */
    @Override 
    public boolean hasBogusName() {
	return false;
    }
    
    public boolean equals(Object o) {
        if (o instanceof ElevatorAlightEdge) {
            ElevatorAlightEdge other = (ElevatorAlightEdge) o;
            return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }
    
    public String toString() {
        return "ElevatorAlightEdge(" + fromv + " -> " + tov + ")";
    }
}
