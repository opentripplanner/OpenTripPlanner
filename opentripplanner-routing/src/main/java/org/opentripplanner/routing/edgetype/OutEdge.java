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
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

/**
 * An edge from the main edge-based street network out to an intersection vertex
 *
 */
public class OutEdge extends AbstractEdge implements EdgeWithElevation, StreetEdge {

    private static final long serialVersionUID = -4922790993642455605L;

    /** No-arg constructor used only for customization -- do not call this unless
     * you know what you are doing */
    public OutEdge() {
        super(null, null);
    }

    public OutEdge(StreetVertex fromv, Vertex tov) {
        super(fromv, tov);
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

    public State traverse(State s0) {
    	return doTraverse(s0, s0.getOptions());
    }

    private State doTraverse(State s0, TraverseOptions options) {
        StreetVertex fromv = ((StreetVertex)this.fromv);
        
        if (!fromv.canTraverse(options)) {
        	// try walking bike since you can't ride it here
        	if (options.getModes().contains(TraverseMode.BICYCLE)) {
        		return doTraverse(s0, options.getWalkingOptions());
        	} else {
        		return null;
        	}
        }

        TraverseMode traverseMode = options.getModes().getNonTransitMode();
        EdgeNarrative en = new FixedModeEdge(this, traverseMode);
        StateEditor s1 = s0.edit(this, en);

        double time = fromv.getEffectiveLength(traverseMode) / options.speed;
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
    
    public boolean equals(Object o) {
        if (o instanceof OutEdge) {
            OutEdge other = (OutEdge) o;
            return other.fromv.equals(fromv) && other.tov.equals(tov);
        }
        return false;
    }

	@Override
	public boolean isNoThruTraffic() {
		return ((StreetVertex) fromv).isNoThruTraffic();
	}
}
