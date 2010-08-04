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

import java.io.Serializable;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * An edge between two StreetVertices. This is the most common edge type in the edge-based street
 * graph.
 * 
 */
public class TurnEdge implements EdgeWithElevation, StreetEdge, Serializable {

    public static final String[] DIRECTIONS = { "north", "northeast", "east", "southeast", "south",
            "southwest", "west", "northwest" };

    private static final long serialVersionUID = -4510937090471837118L;

    public int turnCost;

    StreetVertex fromv;

    StreetVertex tov;

    public TurnEdge(StreetVertex fromv, StreetVertex tov) {
        this.fromv = fromv;
        this.tov = tov;
        turnCost = Math.abs(fromv.outAngle - tov.inAngle);
        if (turnCost > 180) {
            turnCost = 360 - turnCost;
        }
    }

    private static String getDirection(Coordinate a, Coordinate b) {
        double run = b.x - a.x;
        double rise = b.y - a.y;
        double direction = Math.atan2(run, rise);
        int octant = (int) (8 - Math.round(direction * 8 / (Math.PI * 2))) % 8;

        return DIRECTIONS[octant];
    }

    @Override
    public String getDirection() {
        Coordinate[] coordinates = fromv.getGeometry().getCoordinates();
        return getDirection(coordinates[0], coordinates[coordinates.length - 1]);
    }

    @Override
    public double getDistance() {
        return fromv.getLength();
    }

    @Override
    public Geometry getGeometry() {
        return fromv.getGeometry();
    }

    @Override
    public TraverseMode getMode() {
        return TraverseMode.WALK;
    }

    @Override
    public String getName() {
        return fromv.getName();
    }

    @Override
    public Trip getTrip() {
        return null;
    }

    public TraverseResult traverse(State s0, TraverseOptions wo) {

        if (!fromv.canTraverse(wo)) {
            return null;
        }

        double angleLength = fromv.getLength() + turnCost / 20;

        State s1 = s0.clone();
        double time = angleLength / wo.speed;
        double weight = fromv.computeWeight(s0, wo, time);
        s1.walkDistance += angleLength;
        // it takes time to walk/bike along a street, so update state accordingly
        s1.incrementTimeInSeconds((int) time);
        s1.lastEdgeWasStreet = true;
        return new TraverseResult(weight, s1);
    }

    public TraverseResult traverseBack(State s0, TraverseOptions options) {
        if (!fromv.canTraverse(options)) {
            return null;
        }
        double angleLength = fromv.getLength() + turnCost / 20;

        State s1 = s0.clone();
        double time = angleLength / options.speed;
        double weight = fromv.computeWeight(s0, options, time);
        s1.walkDistance += angleLength;
        // time moves *backwards* when traversing an edge in the opposite direction
        s1.incrementTimeInSeconds(-(int) time);
        s1.lastEdgeWasStreet = true;
        return new TraverseResult(weight, s1);
    }

    public Object clone() throws CloneNotSupportedException {
        TurnEdge clone = (TurnEdge) super.clone();
        return clone;
    }

    public String toString() {
        return "TurnEdge( " + fromv + ", " + tov + ")";
    }

    public PackedCoordinateSequence getElevationProfile() {
        return fromv.getElevationProfile();
    }

    @Override
    public Vertex getFromVertex() {
        return fromv;
    }

    @Override
    public Vertex getToVertex() {
        return tov;
    }

    @Override
    public void setFromVertex(Vertex vertex) {
        fromv = (StreetVertex) vertex;
    }

    @Override
    public void setToVertex(Vertex vertex) {
        tov = (StreetVertex) vertex;
    }

    @Override
    public String getName(State state) {
        return getName();
    }

    @Override
    public boolean canTraverse(TraverseOptions options) {
        return fromv.canTraverse(options);
    }
}
