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

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Coordinate;


/**
 * A relatively high cost edge for boarding an elevator.
 * @author mattwigway
 *
 */
public class ElevatorBoardEdge extends Edge {

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
        the_geom = GeometryUtils.getGeometryFactory().createLineString(coords);
    }
    
    @Override
    public State traverse(State s0) { 
        RoutingRequest options = s0.getOptions();
        TraverseMode mode = s0.getNonTransitMode(options);
        
        // don't switch to bike when an elevator occurs, but don't specifically tell the user
        // to switch to walking when the elevator occurs (i.e. if an elevator occurs in the 
        // middle of a biking leg, don't specifically tell the user to dismount and walk - that
        // goes without saying)
        if (mode == TraverseMode.BICYCLE && s0.getBackMode() != TraverseMode.BICYCLE) {
            options = options.getWalkingOptions();
            mode = s0.getNonTransitMode(options);
        }

        StateEditor s1 = s0.edit(this);
        s1.setBackMode(mode);
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
        return the_geom;
    }

    @Override
    public String getName() {
        // TODO: i18n
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
