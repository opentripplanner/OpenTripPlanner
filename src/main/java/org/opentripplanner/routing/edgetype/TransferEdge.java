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

import org.opentripplanner.routing.core.RoutingRequest;
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

    private static final long serialVersionUID = 2L;

    private double distance;

    private LineString geometry = null;

    private boolean wheelchairAccessible = true;

    /**
     * Creates a new Transfer edge.
     * @param fromv     the Vertex where the transfer originates
     * @param tov       the Vertex where the transfer ends
     * @param distance  the distance in meters from the origin Vertex to the destination
     */
    public TransferEdge(TransitStationStop fromv, TransitStationStop tov, double distance) {
        super(fromv, tov);
        this.distance = distance;
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

    @Override
    public State traverse(State s0) {
        /* Disallow chaining of transfer edges. TODO: This should really be guaranteed by the PathParser
           but the default Pathparser is currently very hard to read because
           we need a complement operator. */

        // Forbid taking shortcuts composed of two transfers in a row
        if (s0.backEdge instanceof TransferEdge) {
            return null;
        }
        if (s0.backEdge instanceof StreetTransitLink) {
            return null;
        }
        if(distance > s0.getOptions().maxTransferWalkDistance) {
            return null;
        }
        if (s0.getOptions().wheelchairAccessible && !wheelchairAccessible) {
            return null;
        }
        // Only transfer right after riding a vehicle.
        RoutingRequest rr = s0.getOptions();
        StateEditor se = s0.edit(this);
        se.setBackMode(TraverseMode.WALK);
        int time = getTime(rr);
        se.incrementTimeInSeconds(time);
        se.incrementWeight(time * rr.walkReluctance);
        se.incrementWalkDistance(distance);
        return se.makeState();
    }

    @Override
    public double weightLowerBound(RoutingRequest rr) {
        return (getTime(rr) * rr.walkReluctance);
    }

    public void setGeometry(LineString geometry) {
        this.geometry = geometry;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }

    /*
     * Before merge with SimpleTransfer, this class had its own time parameter = min_transfer_time, or distance if min_tranfer_time unset.
     * min_transfer_time is handled in the TransferTable, so now time can be calculated from distance.
     */
    private int getTime(RoutingRequest rr) {
        return (int) Math.ceil(distance / rr.walkSpeed) + 2 * StreetTransitLink.STL_TRAVERSE_COST;
    }
}
