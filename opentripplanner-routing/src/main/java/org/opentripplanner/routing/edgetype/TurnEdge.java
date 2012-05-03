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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.NoThruTrafficState;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.patch.Patch;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TurnVertex;

import com.vividsolutions.jts.geom.Geometry;

/**
 * An edge between two StreetVertices. This is the most common edge type in the edge-based street
 * graph.
 */
public class TurnEdge extends StreetEdge {

    public static final String[] DIRECTIONS = { "north", "northeast", "east", "southeast", "south",
            "southwest", "west", "northwest" };

    private static final long serialVersionUID = 20120229L;

    public int turnCost;

    private List<Patch> patches;

    /**
     * If not null, this turn is prohibited to the modes in the set.
     */
    private Set<TraverseMode> restrictedModes;

/* enforce initialization */
//    /** No-arg constructor used only for customization -- do not call this unless
//     * you know what you are doing */
//    public TurnEdge() {}
    
    public TurnEdge(TurnVertex fromv, TurnVertex tov) {
        super(fromv, tov);
        this.fromv = fromv;
        this.tov = tov;
        turnCost = Math.abs(fromv.outAngle - tov.inAngle);
        if (turnCost > 180) {
            turnCost = 360 - turnCost;
        }
    }

    // TODO: better handling of multiple constructor arg vertex types, specific multilevel vertex type
    public TurnEdge(TurnVertex fromv, StreetVertex tov) {
        super(fromv, tov);
        this.fromv = fromv;
        this.tov = tov;
        turnCost = 0;
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
        return ((TurnVertex) fromv).getLength();
    }

    @Override
    public Geometry getGeometry() {
        return ((TurnVertex) fromv).getGeometry();
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
        return ((TurnVertex) fromv).isRoundabout();
    }

    @Override
    public Trip getTrip() {
        return null;
    }

    public State traverse(State s0) {
        return doTraverse(s0, s0.getOptions());
    }

    public State optimisticTraverse(State s0) {
        return doTraverse(s0, s0.getOptions());
    }

    private boolean turnRestricted(RoutingRequest options) {
        if (restrictedModes == null)
            return false;
        else {
            return options.getModes().isRestricted(restrictedModes);
        }
    }

    private boolean turnRestricted(State s0, RoutingRequest options) {
        if (restrictedModes == null)
            return false;
        else {
            return restrictedModes.contains(s0.getNonTransitMode(options));
        }
    }

    private State doTraverse(State s0, RoutingRequest options) {
        if (turnRestricted(s0, options) && !options.getModes().contains(TraverseMode.WALK)) {
            return null;
        }
        TraverseMode traverseMode = s0.getNonTransitMode(options);
        if (!((TurnVertex) fromv).canTraverse(options, traverseMode)) {
            if (traverseMode == TraverseMode.BICYCLE) {
                // try walking bicycle, since you can't ride it here
                return doTraverse(s0, options.getWalkingOptions());
            }
            return null;
        }

        FixedModeEdge en = new FixedModeEdge(this, traverseMode);
        Set<Alert> wheelchairNotes = ((TurnVertex) fromv).getWheelchairNotes();
        if (options.wheelchairAccessible) {
            en.addNotes(wheelchairNotes);
        }
        StateEditor s1 = s0.edit(this, en);

        switch (s0.getNoThruTrafficState()) {
        case INIT:
            if (((TurnVertex) fromv).isNoThruTraffic()) {
                s1.setNoThruTrafficState(NoThruTrafficState.IN_INITIAL_ISLAND);
            } else {
                s1.setNoThruTrafficState(NoThruTrafficState.BETWEEN_ISLANDS);
            }
            break;
        case IN_INITIAL_ISLAND:
            if (!((TurnVertex) fromv).isNoThruTraffic()) {
                s1.setNoThruTrafficState(NoThruTrafficState.BETWEEN_ISLANDS);
            }
            break;
        case BETWEEN_ISLANDS:
            if (((TurnVertex) fromv).isNoThruTraffic()) {
                s1.setNoThruTrafficState(NoThruTrafficState.IN_FINAL_ISLAND);
            }
            break;
        case IN_FINAL_ISLAND:
            if (!((TurnVertex) fromv).isNoThruTraffic()) {
                // we have now passed entirely through a no thru traffic region,
                // which is
                // forbidden
                return null;
            }
            break;
        }

        double speed = options.getSpeed(s0.getNonTransitMode(options));
        double time = (((TurnVertex) fromv).getEffectiveLength(traverseMode) + turnCost / 20.0) / speed;
        double weight = ((TurnVertex) fromv).computeWeight(s0, options, time);
        s1.incrementWalkDistance(((TurnVertex) fromv).getLength());
        s1.incrementTimeInSeconds((int) Math.ceil(time));
        s1.incrementWeight(weight);
        if (s1.weHaveWalkedTooFar(options))
            return null;

        return s1.makeState();
    }

    public String toString() {
        return "TurnEdge( " + fromv + ", " + tov + ")";
    }

    public PackedCoordinateSequence getElevationProfile() {
        return ((TurnVertex) fromv).getElevationProfile();
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
    public boolean canTraverse(RoutingRequest options) {
    	if (turnRestricted(options) && !options.getModes().contains(TraverseMode.WALK)) {
    		return false;
    	}
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
    public void addPatch(Patch patch) {
        if (patches == null) {
            patches = new ArrayList<Patch>();
        }
        if (!patches.contains(patch)) {
            patches.add(patch);
        }
    }

    @Override
    public List<Patch> getPatches() {
        if (patches == null) {
            return Collections.emptyList();
        }
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
    public Set<Alert> getNotes() {
        return ((TurnVertex) fromv).getNotes();
    }

    public void setRestrictedModes(Set<TraverseMode> modes) {
        this.restrictedModes = modes;
    }

    public Set<TraverseMode> getRestrictedModes() {
        return restrictedModes;
    }

    @Override
    public boolean hasBogusName() {
        return ((TurnVertex) fromv).hasBogusName();
    }

    @Override
    public boolean isNoThruTraffic() {
        return ((TurnVertex) fromv).isNoThruTraffic();
    }

    @Override
    public double weightLowerBound(RoutingRequest options) {
        return timeLowerBound(options) * options.walkReluctance;
    }
    
    @Override
    public double timeLowerBound(RoutingRequest options) {
        return (((TurnVertex) fromv).getLength() + turnCost/20) / options.getSpeedUpperBound();
    }
    
	    private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException {
	        if (fromv == null)
	            System.out.printf("fromv null %s \n", this);

	        if (tov == null)
	            System.out.printf("tov null %s \n", this);
	        
	        out.defaultWriteObject();
	    }

}
