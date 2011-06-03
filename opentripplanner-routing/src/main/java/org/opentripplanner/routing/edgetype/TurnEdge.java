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
import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateData;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.patch.Patch;

import com.vividsolutions.jts.geom.Geometry;

/**
 * An edge between two StreetVertices. This is the most common edge type in the edge-based street
 * graph.
 * 
 */
public class TurnEdge implements DirectEdge, StreetEdge, Serializable {

    public static final String[] DIRECTIONS = { "north", "northeast", "east", "southeast", "south",
            "southwest", "west", "northwest" };

    private static final long serialVersionUID = -4510937090471837118L;

    public int turnCost;

    StreetVertex fromv;

    StreetVertex tov;

	private List<Patch> patches;

    public TurnEdge(StreetVertex fromv, StreetVertex tov) {
        this.fromv = fromv;
        this.tov = tov;
        turnCost = Math.abs(fromv.outAngle - tov.inAngle);
        if (turnCost > 180) {
            turnCost = 360 - turnCost;
        }
    }

    /*
    private static String getDirection(Coordinate a, Coordinate b) {
        double run = b.x - a.x;
        double rise = b.y - a.y;
        double direction = Math.atan2(run, rise);
        int octant = (int) (8 - Math.round(direction * 8 / (Math.PI * 2))) % 8;

        return DIRECTIONS[octant];
    }
    */

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
    public boolean isRoundabout() {
        return fromv.isRoundabout();
    }

    @Override
    public Trip getTrip() {
        return null;
    }

    public TraverseResult traverse(State s0, TraverseOptions options) {
        return doTraverse(s0, options, false);
    }

    public TraverseResult traverseBack(State s0, TraverseOptions options) {
        return doTraverse(s0, options, true);
    }

    private TraverseResult doTraverse(State s0, TraverseOptions options, boolean back) {
        if (!fromv.canTraverse(options)) {
            return tryWalkBike(s0, options, back);
        }
        
        double angleLength = fromv.getLength() + turnCost / 20;

        StateData.Editor s1 = s0.edit();
        double time = angleLength / options.speed;
        double weight = fromv.computeWeight(s0, options, time);
        s1.incrementWalkDistance(fromv.getLength());
        s1.incrementTimeInSeconds((int) (back ? -time : time));
        
        if( EdgeLibrary.weHaveWalkedTooFar(s1, options))
            return null;
        
        return new TraverseResult(weight, s1.createState(), new FixedModeEdge(this, options.getModes().getNonTransitMode()));
    }

    private TraverseResult tryWalkBike(State s0, TraverseOptions options, boolean back) {
        if (options.getModes().contains(TraverseMode.BICYCLE)) {
            return doTraverse(s0, options.getWalkingOptions(), back);
        }
        return null;
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
    public boolean canTraverse(TraverseOptions options) {
        return fromv.canTraverse(options);
    }

    @Override
    public double getLength() {
        return fromv.getLength();
    }

    @Override
    public PackedCoordinateSequence getElevationProfile(double start, double end) {
        return fromv.getElevationProfile(start, end);
    }

    @Override
    public StreetTraversalPermission getPermission() {
        return fromv.getPermission();
    }

    @Override
    public void setElevationProfile(PackedCoordinateSequence elev) {
        fromv.setElevationProfile(elev);
    }
    
    public boolean equals(Object o) {
        if (o instanceof TurnEdge) {
            TurnEdge other = (TurnEdge) o;
            return other.fromv.equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return fromv.hashCode() * 31 + tov.hashCode();
    }
    
	@Override
	public void addPatch(Patch patch) {
		if (patches == null) {
			patches = new ArrayList<Patch>();
		}
		patches.add(patch);
	}

	@Override
	public List<Patch> getPatches() {
		return patches;
	}
	
	@Override
	public void removePatch(Patch patch) {
		if (patches.size() == 1) {
			patches = null;
		} else {
			patches.remove(patch);
		}
	}

	@Override
	public String getNote() {
		return fromv.getNote();
	}
}
