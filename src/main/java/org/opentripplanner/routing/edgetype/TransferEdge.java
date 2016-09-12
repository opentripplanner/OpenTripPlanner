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
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import com.vividsolutions.jts.geom.LineString;
import java.util.Locale;

/**
 * A transfer directly between two stops without using the street network.
 *
 */
public class TransferEdge extends Edge {

    private static final long serialVersionUID = 1L;
    
    int time = 0;
    
    double distance;

    private LineString geometry = null;

    private boolean wheelchairAccessible = true;

    /**
     * @see Transfer(Vertex, Vertex, double, int)
     */
    public TransferEdge(TransitStationStop fromv, TransitStationStop tov, double distance) {
        this(fromv, tov, distance, (int) distance);
    }
    
    /**
     * Creates a new Transfer edge.
     * @param fromv     the Vertex where the transfer originates
     * @param tov       the Vertex where the transfer ends
     * @param distance  the distance in meters from the origin Vertex to the destination
     * @param time      the minimum time in seconds it takes to complete this transfer
     */
    public TransferEdge(TransitStationStop fromv, TransitStationStop tov, double distance, int time) {
        super(fromv, tov);
        this.distance = distance;
        this.time = time; 
    }

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return distance;
    }

    public LineString getGeometry() {
        return geometry;
    }

    public TraverseMode getMode() {
        return TraverseMode.WALK;
    }

    public String getName() {
        return "Transfer";
    }

    @Override
    public String getName(Locale locale) {
        //TODO: localize
        return this.getName();
    }

    public State traverse(State s0) {
        /* Disallow chaining of transfer edges. TODO: This should really be guaranteed by the PathParser
           but the default Pathparser is currently very hard to read because
           we need a complement operator. */
        if (s0.getBackEdge() instanceof TransferEdge) return null;
        if (s0.getOptions().wheelchairAccessible && !wheelchairAccessible) return null;
        if (this.getDistance() > s0.getOptions().maxTransferWalkDistance) return null;
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(time);
        s1.incrementWeight(time);
        s1.setBackMode(TraverseMode.WALK);
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
