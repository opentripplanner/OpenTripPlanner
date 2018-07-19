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
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.StreetVertex;
import java.util.LinkedList;
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
        setCarNetworks(parentEdge.getCarNetworks());
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
     * Return a subset of the parent elevation profile.
     */
    @Override
    public PackedCoordinateSequence getElevationProfile() {
        PackedCoordinateSequence parentElev = parentEdge.getElevationProfile();
        if (parentElev == null) return null;

        // Compute the linear-reference bounds of the partial edge as fractions of the parent edge
        LocationIndexedLine line = new LocationIndexedLine(parentEdge.getGeometry());
        double startFraction =line.indexOf(this.getGeometry().getStartPoint().getCoordinate()).getSegmentFraction();
        double endFraction = line.indexOf(this.getGeometry().getEndPoint().getCoordinate()).getSegmentFraction();
        if (endFraction == 0) endFraction = 1;

        double parentDistance = parentEdge.getDistance();
        double distanceAdjust = this.getDistance() / ((endFraction - startFraction) * parentDistance);

        // Iterate through each entry of the elevation profile for the full parent edge
        Coordinate parentElevCoords[] = parentElev.toCoordinateArray();
        List<Coordinate> partialElevCoords = new LinkedList<>();
        boolean inPartialEdge = false;
        double startOffset = startFraction * parentDistance;
        for (int i = 1; i < parentElevCoords.length; i++) {
            // compute the fraction range covered by this entry in the elevation profile
            double x1 = parentElevCoords[i - 1].x;
            double x2 = parentElevCoords[i].x;
            double y1 = parentElevCoords[i - 1].y;
            double y2 = parentElevCoords[i].y;
            double f1 = x1 / parentDistance;
            double f2 = x2 / parentDistance;
            if (f2 > 1) f2 = 1;

            // Check if the partial edge begins in current section of the elevation profile
            if (startFraction >= f1 && startFraction < f2) {
                // Compute and add the interpolated elevation coordinate
                double pct = (startFraction - f1) / (f2 - f1);
                double x = x1 + pct * (x2 - x1);
                double y = y1 + pct * (y2 - y1);
                partialElevCoords.add(new Coordinate((x - startOffset) * distanceAdjust, y));

                // We are now "in" the partial-edge portion of the parent edge
                inPartialEdge = true;
            }

            // Check if the partial edge ends in current section of the elevation profile
            if (endFraction >= f1 && endFraction < f2) {
                // Compute and add the interpolated elevation coordinate
                double pct = (endFraction - f1) / (f2 - f1);
                double x = x1 + pct * (x2 - x1);
                double y = y1 + pct * (y2 - y1);
                partialElevCoords.add(new Coordinate((x - startOffset) * distanceAdjust, y));

                // This is the end of the partial edge, so we can end the iteration
                break;
            }

            if (inPartialEdge) {
                Coordinate c = new Coordinate((x2 - startOffset) * distanceAdjust, y2);
                partialElevCoords.add(c);
            }

        }

        Coordinate coords[] = partialElevCoords.toArray(new Coordinate[partialElevCoords.size()]);
        return new PackedCoordinateSequence.Double(coords);
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
