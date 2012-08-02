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

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.AbstractEdge;
import org.opentripplanner.routing.graph.Vertex;

import com.vividsolutions.jts.geom.LineString;

/**
 * A transfer directly between two stops without using the street network.
 *
 */
public class TransferEdge extends AbstractEdge {

    private static final long serialVersionUID = 1L;
    
    int time = 0;
    
    double distance;

    private LineString geometry = null;

    private boolean wheelchairAccessible = true;

    /**
     * @see Transfer(Vertex, Vertex, double, int)
     */
    public TransferEdge(Vertex fromv, Vertex tov, double distance) {
        super(fromv, tov);
        this.distance = distance;
        this.time = (int) distance; //(int) distance * 3;
    }
    
    /**
     * Creates a new Transfer edge.
     * @param fromv     the Vertex where the transfer originates
     * @param tov       the Vertex where the transfer ends
     * @param distance  the distance in meters from the origin Vertex to the destination
     * @param time      the minimum time in seconds it takes to complete this transfer
     */
    public TransferEdge(Vertex fromv, Vertex tov, double distance, int time) {
        super(fromv, tov);
        this.distance = distance;
        this.time = time; 
    }

    public String getDirection() {
        // TODO Auto-generated method stub
        return null;
    }

    public double getDistance() {
        return distance;
    }

    public LineString getGeometry() {
        return geometry;
    }

    public TraverseMode getMode() {
        return TraverseMode.TRANSFER;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return "transfer";
    }

    public State traverse(State s0) {
        if (s0.getOptions().wheelchairAccessible && !wheelchairAccessible) {
            return null;
        }
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(time);
        s1.incrementWeight(time);
        return s1.makeState();
    }

    public void setGeometry(LineString geometry) {
        this.geometry  = geometry;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }

}
