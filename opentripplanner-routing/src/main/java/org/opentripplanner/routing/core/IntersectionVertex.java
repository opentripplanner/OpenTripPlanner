/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.opentripplanner.routing.core;

import java.util.Vector;

import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.edgetype.Turn;
import org.opentripplanner.routing.impl.DistanceLibrary;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class IntersectionVertex extends OneStreetVertex implements StreetIntersectionVertex {

    private static final long serialVersionUID = 364261663335739528L;

    public Intersection intersection;

    public int angle;

    public IntersectionVertex(Intersection intersection, Geometry geometry, boolean edgePointsIn) {
        this.intersection = intersection;
        intersection.vertices.add(this);
        double angleR;
        if (edgePointsIn) {
            angleR = DirectionUtils.getInstance().getLastAngle(geometry);
        } else {
            angleR = Math.PI + DirectionUtils.getInstance().getFirstAngle(geometry);
        }
        angle = ((int) (180 * angleR / Math.PI) + 180 + 360) % 360;
    }

    public double distance(Vertex v) {

        double xd = v.getX() - getX();
        double yd = v.getY() - getY();
        return Math.sqrt(xd * xd + yd * yd) * GenericVertex.METERS_PER_DEGREE_AT_EQUATOR
                * GenericVertex.COS_MAX_LAT;

        /* This is more accurate but slower */
        // return GtfsLibrary.distance(y, x, v.y, v.x);
    }

    public double distance(Coordinate c) {
        return DistanceLibrary.distance(getY(), getX(), c.y, c.x);
    }

    public Coordinate getCoordinate() {
        return new Coordinate(getX(), getY());
    }

    public int getDegreeOut() {
        return getOutgoing().size();
    }

    public int getDegreeIn() {
        return getIncoming().size();
    }

    public void addIncoming(Edge ee) {
        throw new UnsupportedOperationException("Incoming and outgoing edges are only inStreet and outStreet, or those from intersection");
    }

    public void addOutgoing(Edge ee) {
        throw new UnsupportedOperationException("Incoming and outgoing edges are only inStreet and outStreet, or those from intersection");
    }

    public String toString() {
        return "<" + getName() + " " + this.intersection.getDegree() + ">";
    }

    public double getX() {
        return intersection.x;
    }

    public double getY() {
        return intersection.y;
    }

    public Vector<Edge> getOutgoing() {
        Vector<Edge> outgoing = intersection.getOutgoing(this);
        if (outStreet != null) {
            outgoing.add(outStreet);
        }
        return outgoing;
    }

    public Vector<Edge> getIncoming() {
        Vector<Edge> incoming = intersection.getIncoming(this);
        if (inStreet != null) {
            incoming.add(inStreet);
        }
        return incoming;
    }

    @Override
    public String getLabel() {
        return intersection.label + " at " + angle;
    }

    /**
     * Try to compute a more user-friendly name for this intersection than just the label. We do
     * this on the fly because this method is never called for most intersections and we want to
     * avoid the memory overhead of storing an extra string for every intersection unnecessarily.
     *
     * TODO: Figure out a better way of doing this. The code below is horrendously ugly and it runs
     * in O(n^2) (though n = number of streets incident to this intersection, and the typical case
     * running time is much less than n^2).
     */
    @Override
    public String getName() {
        String firstIncidentStreet = null;
        for (Edge e : getOutgoing()) {
            if (e instanceof Street) {
                firstIncidentStreet = e.getName();
                for (Edge t : getOutgoing()) {
                    if (t instanceof Turn) {

                        if (((Turn) t).turnAngle != 0) {
                            for (Edge e2 : t.getToVertex().getIncoming()) {
                                if (e2 instanceof Street) {
                                    if (e2.getName() != firstIncidentStreet) {
                                        return firstIncidentStreet + " & " + e2.getName();
                                    }
                                }
                            }
                            for (Edge e2 : t.getToVertex().getOutgoing()) {
                                if (e2 instanceof Street) {
                                    if (e2.getName() != firstIncidentStreet) {
                                        return firstIncidentStreet + " & " + e2.getName();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Can't come up with a better name, fall back to label.
        return getLabel();
    }

    @Override
    public String getStopId() {
        return null;
    }

}
