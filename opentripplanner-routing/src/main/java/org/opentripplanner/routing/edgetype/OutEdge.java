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

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * An edge from the main edge-based street network out to an intersection vertex
 *
 */
public class OutEdge extends AbstractEdge implements EdgeWithElevation, StreetEdge {

    private static final long serialVersionUID = -4922790993642455605L;


    public OutEdge(StreetVertex fromv, Vertex tov) {
        super(fromv, tov);
    }
    private static String getDirection(Coordinate a, Coordinate b) {
        double run = b.x - a.x;
        double rise = b.y - a.y;
        double direction = Math.atan2(run, rise);
        int octant = (int) (8 + Math.round(direction * 8 / (Math.PI * 2))) % 8;

        return TurnEdge.DIRECTIONS[octant];
    }

    @Override
    public String getDirection() {
        Coordinate[] coordinates = ((StreetVertex) fromv).getGeometry().getCoordinates();
        return getDirection(coordinates[0], coordinates[coordinates.length - 1]);
    }

    @Override
    public double getDistance() {
        return ((StreetVertex)getFromVertex()).getLength();
    }

    @Override
    public Geometry getGeometry() {
        return ((StreetVertex)fromv).getGeometry();
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
        StreetVertex fromv = ((StreetVertex)this.fromv);
        
        if (!fromv.canTraverse(wo)) {
            return null;
        }

        State s1 = s0.clone();
        double time = fromv.getLength() / wo.speed;
        double weight = fromv.computeWeight(s0, wo, time);
        s1.walkDistance += fromv.getLength();
        // it takes time to walk/bike along a street, so update state accordingly
        s1.incrementTimeInSeconds((int) time);
        s1.lastEdgeWasStreet = true;
        return new TraverseResult(weight, s1);
    }

    public TraverseResult traverseBack(State s0, TraverseOptions options) {
        StreetVertex fromv = ((StreetVertex)this.fromv);
        
        if (!fromv.canTraverse(options)) {
            return null;
        }

        State s1 = s0.clone();
        double time = fromv.getLength() / options.speed;
        double weight = fromv.computeWeight(s0, options, time);
        s1.walkDistance += fromv.getLength();
        // time moves *backwards* when traversing an edge in the opposite direction
        s1.incrementTimeInSeconds(-(int) time);
        s1.lastEdgeWasStreet = true;
        return new TraverseResult(weight, s1);
    }

    public String toString() {
        return "OutEdge( " + fromv + ", " + tov + ")";
    }

    public PackedCoordinateSequence getElevationProfile() {
        return ((StreetVertex) fromv).getElevationProfile();
    }

    public boolean canTraverse(TraverseOptions options) {
        return ((StreetVertex) fromv).canTraverse(options);
    }
    @Override
    public double getLength() {
        return ((StreetVertex) fromv).getLength();
    }
    @Override
    public PackedCoordinateSequence getElevationProfile(double start, double end) {
        return ((StreetVertex) fromv).getElevationProfile(start, end);
    }
    @Override
    public StreetTraversalPermission getPermission() {
        return ((StreetVertex) fromv).getPermission();
    }

    @Override
    public void setElevationProfile(PackedCoordinateSequence elev) {
        ((StreetVertex)fromv).setElevationProfile(elev);
    }
}
