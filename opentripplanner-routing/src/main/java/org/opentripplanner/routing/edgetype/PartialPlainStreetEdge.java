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

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.StreetVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a sub-segment of a StreetEdge.
 * 
 * @author avi
 */
public class PartialPlainStreetEdge extends PlainStreetEdge {

    private static final long serialVersionUID = 1L;

    /**
     * The edge on which this lies.
     */
    @Setter
    @Getter
    private StreetEdge parentEdge;

    public PartialPlainStreetEdge(StreetEdge parentEdge, StreetVertex v1, StreetVertex v2,
            LineString geometry, String name, double length, StreetTraversalPermission permission,
            boolean back) {
        super(v1, v2, geometry, name, length, permission, back, parentEdge.getCarSpeed());

        this.parentEdge = parentEdge;
    }
    
    /**
     * Simplifies construction by copying some stuff from the parentEdge.
     */
    public PartialPlainStreetEdge(StreetEdge parentEdge, StreetVertex v1, StreetVertex v2,
            LineString geometry, String name, double length) {
        this(parentEdge, v1, v2, geometry, name, length, parentEdge.getPermission(), false);
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
     * This implementation makes it so that TurnRestrictions on the parent edge are applied to this edge as well.
     */
    @Override
    public boolean isEquivalentTo(Edge e) {
        return (e == this || e == parentEdge);
    }
    
    @Override
    public boolean isReverseOf(Edge e) {
        Edge other = e;
        if (e instanceof PartialPlainStreetEdge) {
            other = ((PartialPlainStreetEdge) e).getParentEdge();
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
    
    @Override
    public String toString() {
        return "PartialPlainStreetEdge(" + this.getName() + ", " + this.getFromVertex() + " -> "
                + this.getToVertex() + " length=" + this.getLength() + " carSpeed="
                + this.getCarSpeed() + " parentEdge=" + parentEdge + ")";
    }

}
