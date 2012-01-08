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

import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * An edge representing an elevator between two floors. It generates its own narratives.
 * Multi-story elevators are represented by a bunch of these. 
 * Code copied from FreeEdge 2012-01-05.
 * @author mattwigway
 *
 */
public class ElevatorEdge extends PlainStreetEdge {

    // TODO: do I need to change this?
    private static final long serialVersionUID = 3925814840369402222L;

    public ElevatorEdge(Vertex from, Vertex to, StreetTraversalPermission permission) {
	// null geometry, name is "elevator" (do these names have to be unique?), length
	// is 0 (2d), permission may be WALK or WALK_AND_BICYCLE, generally speaking, in rare
	// cases it may also include automobiles. And, I've never heard of a one-way elevator,
	// (except in books by Louis Sachar) so back is always true.
	// I'm not particularly keen on the nested mess either, but super has to be the first 
	// call in the constructor, and geometry in PlainStreetEdge is private.
        super(from, to, 
	      new GeometryFactory().createLineString(
						     new Coordinate[] { from.getCoordinate(), 
									to.getCoordinate()}
						     ), 
	      "elevator", 0, permission, true);
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
	// TODO: should this be vertical distance? I think most other edges do not report their
	// vertical distance (such as stairs), and we don't really want the vertical distance
	// added to the trip distance.
        return 0;
    }

    @Override
    public TraverseMode getMode() {
	// TODO: elevators can also be used by cyclists in some cases.
	// but cyclists can never *ride their bikes* in an elevator
        return TraverseMode.WALK;
    }

    @Override
    public String getName() {
        return null;
    }
    
    public boolean equals(Object o) {
        if (o instanceof ElevatorEdge) {
            ElevatorEdge other = (ElevatorEdge) o;
            return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }
    
    public String toString() {
        return "ElevatorEdge(" + fromv + " -> " + tov + ")";
    }
}
