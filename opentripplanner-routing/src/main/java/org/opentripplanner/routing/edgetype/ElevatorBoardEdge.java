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
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Coordinate;


/**
 * A relatively high cost edge for boarding an elevator.
 * @author mattwigway
 *
 */
public class ElevatorBoardEdge extends AbstractEdge {

    private static final long serialVersionUID = 3925814840369402222L;

    /**
     * The polyline geometry of this edge.
     * It's generally a polyline with two coincident points, but some elevators have horizontal
     * dimension, e.g. the ones on the Eiffel Tower.
     */
    private Geometry the_geom;

    public ElevatorBoardEdge(Vertex from, Vertex to) {
        super(from, to);

        // set up the geometry
        Coordinate[] coords = new Coordinate[2];
        coords[0] = new Coordinate(from.getX(), from.getY());
        coords[1] = new Coordinate(to.getX(), to.getY());
        // TODO: SRID?
        the_geom = new GeometryFactory().createLineString(coords);
    }
    
    @Override
    public State traverse(State s0) {
        EdgeNarrative en = new FixedModeEdge(this, s0.getNonTransitMode(s0.getOptions())); 
        TraverseOptions options = s0.getOptions();

        StateEditor s1 = s0.edit(this, en);
        s1.incrementWeight(options.elevatorBoardCost);
        s1.incrementTimeInSeconds(options.elevatorBoardTime);
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
        return "Elevator";
    }

    /** 
     * Since board edges always are called Elevator,
     * the name is utterly and completely bogus but is never included
     * in plans..
     */
    @Override
    public boolean hasBogusName() {
        return true;
    }
    
    public String toString() {
        return "ElevatorBoardEdge(" + fromv + " -> " + tov + ")";
    }
}
