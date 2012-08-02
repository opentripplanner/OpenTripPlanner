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
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.util.ElevationProfileSegment;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TurnVertex;

import com.vividsolutions.jts.geom.LineString;

/**
 * An edge from the main edge-based street network out to an intersection vertex
 *
 */
public class OutEdge extends StreetEdge {

    private static final long serialVersionUID = -4922790993642455605L;

    /**
     * No-arg constructor used only for customization -- do not call this unless you know what you
     * are doing
     */
    public OutEdge() {
        super(null, null);
    }

    public OutEdge(TurnVertex fromv, StreetVertex tov) {
        super(fromv, tov);
    }

    @Override
    public double getDistance() {
        return ((TurnVertex) getFromVertex()).getLength();
    }

    @Override
    public LineString getGeometry() {
        return ((TurnVertex) fromv).getGeometry();
    }

    @Override
    public TraverseMode getMode() {
        return TraverseMode.WALK;
    }

    @Override
    public String getName() {
        return ((TurnVertex) fromv).getName();
    }

    @Override
    public Trip getTrip() {
        return null;
    }

    public State traverse(State s0) {
        return doTraverse(s0, s0.getOptions());
    }

    private State doTraverse(State s0, RoutingRequest options) {
        TurnVertex fromv = ((TurnVertex) this.fromv);
        TraverseMode traverseMode = s0.getNonTransitMode(options);
        if (!fromv.canTraverse(options, traverseMode)) {
            // try walking bike since you can't ride it here
            if (traverseMode == TraverseMode.BICYCLE) {
                return doTraverse(s0, options.getWalkingOptions());
            } else {
                return null;
            }
        }

        FixedModeEdge en = new FixedModeEdge(this, traverseMode);
        if (fromv.getWheelchairNotes() != null && options.wheelchairAccessible) {
            en.addNotes(fromv.getWheelchairNotes());
        }
        StateEditor s1 = s0.edit(this, en);

        double speed = options.getSpeed(traverseMode);
        double time = fromv.getEffectiveLength(traverseMode) / speed;
        double weight = fromv.computeWeight(s0, options, time);
        s1.incrementWalkDistance(fromv.getLength());
        s1.incrementTimeInSeconds((int) time);
        s1.incrementWeight(weight);
        if (s1.weHaveWalkedTooFar(options))
            return null;

        return s1.makeState();
    }

    public String toString() {
        return "OutEdge( " + fromv + ", " + tov + ")";
    }

    public PackedCoordinateSequence getElevationProfile() {
        return ((TurnVertex) fromv).getElevationProfile();
    }

    @Override
    public boolean canTraverse(RoutingRequest options) {
        return ((TurnVertex) fromv).canTraverse(options);
    }

    @Override
    public double getLength() {
        return ((TurnVertex) fromv).getLength();
    }

    @Override
    public PackedCoordinateSequence getElevationProfile(double start, double end) {
        return ((TurnVertex) fromv).getElevationProfile(start, end);
    }

    @Override
    public StreetTraversalPermission getPermission() {
        return ((TurnVertex) fromv).getPermission();
    }

    @Override
    public boolean setElevationProfile(PackedCoordinateSequence elev, boolean computed) {
        return ((TurnVertex) fromv).setElevationProfile(elev, computed);
    }

    @Override
    public boolean isNoThruTraffic() {
        return ((TurnVertex) fromv).isNoThruTraffic();
    }

    @Override
    public int getStreetClass() {
        //this is always safe as it can appear anywhere in a path
        return CLASS_OTHERPATH; 
    }

    @Override
    public ElevationProfileSegment getElevationProfileSegment() {
        return ((TurnVertex) fromv).getElevationProfileSegment();
    }

    @Override
    public boolean isWheelchairAccessible() {
        return ((TurnVertex) fromv).isWheelchairAccessible();
    }
}
