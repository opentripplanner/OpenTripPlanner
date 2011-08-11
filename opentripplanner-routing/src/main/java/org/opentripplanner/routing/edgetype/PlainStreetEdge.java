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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.NoThruTrafficState;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * This represents a street segment. This is unusual in an edge-based graph, but happens when we
 * have to split a set of turns to accommodate a transit stop.
 * 
 * @author novalis
 * 
 */
public class PlainStreetEdge extends AbstractEdge implements StreetEdge {

    private static final long serialVersionUID = 1L;

    private PackedCoordinateSequence elevationProfile;

    private double length;

    private LineString geometry;

    private String name;

    private double slopeSpeedEffectiveLength;

    private double bicycleSafetyEffectiveLength;

    private double slopeCostEffectiveLength;

    private boolean wheelchairAccessible = true;

    private double maxSlope;

    private StreetTraversalPermission permission;

    private String id;

    private boolean crossable = true;

    private boolean slopeOverride = false;

    public boolean back;
    
    private boolean roundabout = false;

    private Set<String> notes;

	private boolean hasBogusName;

	private boolean noThruTraffic;
    
    public PlainStreetEdge(Vertex v1, Vertex v2, LineString geometry, String name, double length,
            StreetTraversalPermission permission, boolean back) {
        super(v1, v2);
        this.geometry = geometry;
        this.length = length;
        slopeSpeedEffectiveLength = length;
        bicycleSafetyEffectiveLength = length;
        slopeCostEffectiveLength = length;
        this.name = name;
        this.permission = permission;
        this.back = back;
    }

    @Override
    public boolean canTraverse(TraverseOptions options) {
        if (options.wheelchairAccessible) {
            if (!wheelchairAccessible) {
                return false;
            }
            if (maxSlope > options.maxSlope) {
                return false;
            }
        }

        if (options.getModes().getWalk() && permission.allows(StreetTraversalPermission.PEDESTRIAN)) {
            return true;
        }

        if (options.getModes().getBicycle() && permission.allows(StreetTraversalPermission.BICYCLE)) {
            return true;
        }

        if (options.getModes().getCar() && permission.allows(StreetTraversalPermission.CAR)) {
            return true;
        }

        return false;
    }

    @Override
    public PackedCoordinateSequence getElevationProfile() {
        return elevationProfile;
    }
    @Override
    public void setElevationProfile(PackedCoordinateSequence elev) {
        if (elev == null) {
            return;
        }
        if (slopeOverride) {
            elev = new PackedCoordinateSequence.Float(new Coordinate[] { elev.getCoordinate(0),elev.getCoordinate((elev.size()-1))},
                    2);
        }
        elevationProfile = elev;
        P2<Double> result = StreetVertex.computeSlopeCost(elev, getName());
        slopeCostEffectiveLength = result.getFirst();
        maxSlope = result.getSecond();
    }

    @Override
    public double getDistance() {
        return length;
    }

    @Override
    public LineString getGeometry() {
        return geometry;
    }

    @Override
    public TraverseMode getMode() {
        return TraverseMode.WALK;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public State traverse(State s0) {
        return doTraverse(s0, s0.getOptions());
    }

    private State doTraverse(State s0, TraverseOptions options) {
        if (!canTraverse(options)) {
            if (options.getModes().contains(TraverseMode.BICYCLE)) {
            	// try walking bike since you can't ride here
                return doTraverse(s0, options.getWalkingOptions());
            }
            return null;
        }
        double time = length / options.speed;
        double weight;
        if (options.wheelchairAccessible) {
            weight = getSlopeSpeedEffectiveLength() / options.speed;
        } else if (options.getModes().contains(TraverseMode.BICYCLE)) {
            switch (options.optimizeFor) {
            case SAFE:
            	weight = bicycleSafetyEffectiveLength / options.speed;
            	break;
            case GREENWAYS:            	
                weight = bicycleSafetyEffectiveLength / options.speed;
                if (bicycleSafetyEffectiveLength / length <= StreetVertex.GREENWAY_SAFETY_FACTOR) {
                	//greenways are treated as even safer than they really are
                	weight *= 0.66;
                }
                break;
            case FLAT:
                weight = slopeCostEffectiveLength;
                break;
            case QUICK:
                weight = getSlopeSpeedEffectiveLength() / options.speed;
                break;
            default:
                // TODO: greenways
                weight = length / options.speed;
            }
        } else {
            weight = time;
        }
        weight *= options.walkReluctance;
        EdgeNarrative en = new FixedModeEdge(this, options.getModes().getNonTransitMode());
        StateEditor s1 = s0.edit(this, en);

        if (options.getModes().getNonTransitMode().equals(TraverseMode.CAR)) {
            switch (s0.getNoThruTrafficState()) {
            case INIT:
                if (isNoThruTraffic()) {
                    s1.setNoThruTrafficState(NoThruTrafficState.IN_INITIAL_ISLAND);
                } else {
                    s1.setNoThruTrafficState(NoThruTrafficState.BETWEEN_ISLANDS);
                }
                break;
            case IN_INITIAL_ISLAND:
                if (!isNoThruTraffic()) {
                    s1.setNoThruTrafficState(NoThruTrafficState.BETWEEN_ISLANDS);
                }
                break;
            case BETWEEN_ISLANDS:
                if (isNoThruTraffic()) {
                    s1.setNoThruTrafficState(NoThruTrafficState.IN_FINAL_ISLAND);
                }
                break;
            case IN_FINAL_ISLAND:
                if (!isNoThruTraffic()) {
                    // we have now passed entirely through a no thru traffic region, which is
                    // forbidden
                    return null;
                }
                break;
            }
        }

        s1.incrementWalkDistance(length);
        s1.incrementTimeInSeconds((int) time);
        s1.incrementWeight(weight);
        if (s1.weHaveWalkedTooFar(options))
            return null;
        
        return s1.makeState();
    }

    public void setSlopeSpeedEffectiveLength(double slopeSpeedEffectiveLength) {
        this.slopeSpeedEffectiveLength = slopeSpeedEffectiveLength;
    }

    public double getSlopeSpeedEffectiveLength() {
        return slopeSpeedEffectiveLength;
    }

    public void setBicycleSafetyEffectiveLength(double bicycleSafetyEffectiveLength) {
        this.bicycleSafetyEffectiveLength = bicycleSafetyEffectiveLength;
    }

    public double getBicycleSafetyEffectiveLength() {
        return bicycleSafetyEffectiveLength;
    }

    public double getLength() {
        return length;
    }

    public StreetTraversalPermission getPermission() {
        return permission;
    }

    public void setPermission(StreetTraversalPermission permission) {
        this.permission = permission;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public String getId() {
        return id;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        id = null; // this is only used during graph construction
        out.writeObject(this);
    }

    public boolean isCrossable() {
        return crossable;
    }

    public boolean getSlopeOverride() {
        return slopeOverride ;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public PackedCoordinateSequence getElevationProfile(double start, double end) {
        if (elevationProfile == null) {
            return null;
        }
        List<Coordinate> coordList = new LinkedList<Coordinate>();

        if (start < 0)
            start = 0;
        if (end > length)
            end = length;

        for (Coordinate coord : elevationProfile.toCoordinateArray()) {
            if (coord.x >= start && coord.x <= end) {
                coordList.add(new Coordinate(coord.x - start, coord.y));
            }
        }

        Coordinate coordArr[] = new Coordinate[coordList.size()];
        return new PackedCoordinateSequence.Float(coordList.toArray(coordArr), 2);
    }

    public void setSlopeOverride(boolean slopeOverride) {
        this.slopeOverride = slopeOverride;
    }

    public void setRoundabout(boolean roundabout) {
        this.roundabout = roundabout;
    }

    public boolean isRoundabout() {
        return roundabout;
    }

    public Set<String> getNotes() {
    	return notes;
    }
    
    public void setNote(Set<String> notes) {
    	this.notes = notes;
    }
    
    @Override
    public String toString() {
        return "PlainStreetEdge(" + fromv + " -> " + tov + ")";
    }

    public boolean hasBogusName() {
        return hasBogusName;
    }
    
    public void setBogusName(boolean hasBogusName) {
        this.hasBogusName = hasBogusName;
    }

    public void setNoThruTraffic(boolean noThruTraffic) {
        this.noThruTraffic = noThruTraffic;
    }

    public boolean isNoThruTraffic() {
        return noThruTraffic;
    }

    public boolean equals(Object o) {
        if (!(o instanceof PlainStreetEdge)) {
            return false;
        }
        PlainStreetEdge pso = (PlainStreetEdge) o;
        return pso.back == back && pso.fromv == fromv && pso.tov == tov && pso.length == length && pso.name.equals(name);
    }

    public int hashCode() {
        return (back ? 2 : 0) * fromv.hashCode() * tov.hashCode() * (new Double(length)).hashCode() * name.hashCode();
    }
}
