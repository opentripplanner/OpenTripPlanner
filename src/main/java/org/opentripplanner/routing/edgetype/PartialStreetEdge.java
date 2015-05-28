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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.StreetVertex;

import java.util.List;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;

/**
 * Represents a sub-segment of a StreetEdge.
 *
 * TODO we need a way to make sure all temporary edges are recorded as such and assigned a routingcontext when they are
 * created. That list should probably be in the routingContext itself instead of the created StreetLocation.
 */
public class PartialStreetEdge extends StreetWithElevationEdge {

    private static final long serialVersionUID = 1L;

    /**
     * The edge on which this lies.
     */
    private StreetEdge parentEdge;

    public PartialStreetEdge(StreetEdge parentEdge, StreetVertex v1, StreetVertex v2,
                             LineString geometry, I18NString name, double length) {
        super(v1, v2, geometry, name, length, parentEdge.getPermission(), false);
        setCarSpeed(parentEdge.getCarSpeed());
        this.parentEdge = parentEdge;
    }
    

    //For testing only
    public PartialStreetEdge(StreetEdge parentEdge, StreetVertex v1, StreetVertex v2,
            LineString geometry, String name, double length) {
        this(parentEdge, v1, v2, geometry, new NonLocalizedString(name), length);
    }

    /**
     * Partial edges are always partial.
     */
    @Override
    public boolean isPartial() {
        return true;
    }
    
    /**
     * Have the ID of their parent.
     */
    @Override
    public int getId() {
        return parentEdge.getId();
    }
    
    /**
     * Have the inbound angle of  their parent.
     */
    @Override
    public int getInAngle() {
        return parentEdge.getInAngle();
    }
    
    /**
     * Have the outbound angle of  their parent.
     */
    @Override
    public int getOutAngle() {
        return parentEdge.getInAngle();
    }

    /**
     * Have the turn restrictions of  their parent.
     */
    @Override
    protected List<TurnRestriction> getTurnRestrictions(Graph graph) {
        return graph.getTurnRestrictions(parentEdge);
    }

    /**
     * This implementation makes it so that TurnRestrictions on the parent edge are applied to this edge as well.
     */
    @Override
    public boolean isEquivalentTo(Edge e) {
        return (e == this || e == parentEdge);
    }
    
    @Override
    public boolean isReverseOf(Edge e) {
        Edge other = e;
        if (e instanceof PartialStreetEdge) {
            other = ((PartialStreetEdge) e).parentEdge;
        }
        
        // TODO(flamholz): is there a case where a partial edge has a reverse of its own?
        return parentEdge.isReverseOf(other);
    }
    
    @Override
    public boolean isRoundabout() {
        return parentEdge.isRoundabout();
    }
    
    /**
     * Returns true if this edge is trivial - beginning and ending at the same point.
     */
    public boolean isTrivial() {
        Coordinate fromCoord = this.getFromVertex().getCoordinate();
        Coordinate toCoord = this.getToVertex().getCoordinate();
        return fromCoord.equals(toCoord);
    }
    
    public StreetEdge getParentEdge() {
        return parentEdge;
    }

    @Override
    public String toString() {
        return "PartialStreetEdge(" + this.getName() + ", " + this.getFromVertex() + " -> "
                + this.getToVertex() + " length=" + this.getDistance() + " carSpeed="
                + this.getCarSpeed() + " parentEdge=" + parentEdge + ")";
    }
}
