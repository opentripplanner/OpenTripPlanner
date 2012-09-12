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

import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.TurnRestrictionType;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.util.ElevationProfileSegment;
import org.opentripplanner.routing.vertextype.StreetVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * This represents a street segment. This is unusual in an edge-based graph, but happens when we
 * have to split a set of turns to accommodate a transit stop.
 * 
 * @author novalis
 * 
 */
public class PlainStreetEdge extends StreetEdge implements Cloneable {

    private static final long serialVersionUID = 1L;

    private static final double GREENWAY_SAFETY_FACTOR = 0.1;

    private ElevationProfileSegment elevationProfileSegment;

    private double length;

    private LineString geometry;

    private String name;

    private boolean wheelchairAccessible = true;

    private StreetTraversalPermission permission;

    private String id;

    private int streetClass = CLASS_OTHERPATH;

    public boolean back;
    
    private boolean roundabout = false;

    private Set<Alert> notes;

    private boolean hasBogusName;

    private boolean noThruTraffic;

    /**
     * This street is a staircase
     */
    private boolean stairs;

    private Set<Alert> wheelchairNotes;

    private List<TurnRestriction> turnRestrictions = Collections.emptyList();

    public int inAngle;

    public int outAngle;

    /**
     * No-arg constructor used only for customization -- do not call this unless you know
     * what you are doing
     */
    public PlainStreetEdge() {
        super(null, null);
    }

    // Presently, we have plainstreetedges that connect both IntersectionVertexes and
    // TurnVertexes
    public PlainStreetEdge(StreetVertex v1, StreetVertex v2, LineString geometry, 
            String name, double length,
            StreetTraversalPermission permission, boolean back) {
        super(v1, v2);
        this.geometry = geometry;
        this.length = length;
        this.elevationProfileSegment = new ElevationProfileSegment(length);
        this.name = name;
        this.permission = permission;
        this.back = back;
        if (geometry != null) {
            for (Coordinate c : geometry.getCoordinates()) {
                if (Double.isNaN(c.x)) {
                    System.out.println("DOOM");
                }
            }
            double angleR = DirectionUtils.getLastAngle(geometry);
            outAngle = ((int) (180 * angleR / Math.PI) + 180 + 360) % 360;
            angleR = DirectionUtils.getFirstAngle(geometry);
            inAngle = ((int) (180 * angleR / Math.PI) + 180 + 360) % 360;
        }
    }

    @Override
    public boolean canTraverse(RoutingRequest options) {
        if (options.wheelchairAccessible) {
            if (!wheelchairAccessible) {
                return false;
            }
            if (elevationProfileSegment.getMaxSlope() > options.maxSlope) {
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

    private boolean canTraverse(RoutingRequest options, TraverseMode mode) {
        if (options.wheelchairAccessible) {
            if (!wheelchairAccessible) {
                return false;
            }
            if (elevationProfileSegment.getMaxSlope() > options.maxSlope) {
                return false;
            }
        }
        if (mode == TraverseMode.WALK && permission.allows(StreetTraversalPermission.PEDESTRIAN)) {
            return true;
        }
        if (mode == TraverseMode.BICYCLE && permission.allows(StreetTraversalPermission.BICYCLE)) {
            return true;
        }
        if (mode == TraverseMode.CAR && permission.allows(StreetTraversalPermission.CAR)) {
            return true;
        }
        return false;
    }

    @Override
    public PackedCoordinateSequence getElevationProfile() {
        return elevationProfileSegment.getElevationProfile();
    }

    @Override
    public boolean setElevationProfile(PackedCoordinateSequence elev, boolean computed) {
        return elevationProfileSegment.setElevationProfile(elev, computed, permission.allows(StreetTraversalPermission.CAR));
    }

    @Override
    public boolean isElevationFlattened() {
        return elevationProfileSegment.isFlattened();
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
    public String getName() {
        return name;
    }

    @Override
    public State traverse(State s0) {
        final RoutingRequest options = s0.getOptions();
        return doTraverse(s0, options, s0.getNonTransitMode(options));
    }

    private State doTraverse(State s0, RoutingRequest options, TraverseMode traverseMode) {
        Edge backEdge = s0.getBackEdge();
        if (backEdge != null && 
                (options.arriveBy ? (backEdge.getToVertex() == fromv) : (backEdge.getFromVertex() == tov))) {
            //no illegal U-turns
            return null;
        }
        if (!canTraverse(options, traverseMode)) {
            if (traverseMode == TraverseMode.BICYCLE) {
                // try walking bike since you can't ride here
                return doTraverse(s0, options.getWalkingOptions(), TraverseMode.WALK);
            }
            return null;
        }
        double speed = options.getSpeed(traverseMode);
        double time = length / speed;
        double weight;
        if (options.wheelchairAccessible) {
            weight = elevationProfileSegment.getSlopeSpeedEffectiveLength() / speed;
        } else if (traverseMode.equals(TraverseMode.BICYCLE)) {
            time = elevationProfileSegment.getSlopeSpeedEffectiveLength() / speed;
            switch (options.optimize) {
            case SAFE:
                weight = elevationProfileSegment.getBicycleSafetyEffectiveLength() / speed;
                break;
            case GREENWAYS:
                weight = elevationProfileSegment.getBicycleSafetyEffectiveLength() / speed;
                if (elevationProfileSegment.getBicycleSafetyEffectiveLength() / length <= GREENWAY_SAFETY_FACTOR) {
                    // greenways are treated as even safer than they really are
                    weight *= 0.66;
                }
                break;
            case FLAT:
                /* see notes in StreetVertex on speed overhead */
                weight = length / speed + elevationProfileSegment.getSlopeWorkCost();
                break;
            case QUICK:
                weight = elevationProfileSegment.getSlopeSpeedEffectiveLength() / speed;
                break;
            case TRIANGLE:
                double quick = elevationProfileSegment.getSlopeSpeedEffectiveLength();
                double safety = elevationProfileSegment.getBicycleSafetyEffectiveLength();
                double slope = elevationProfileSegment.getSlopeWorkCost();
                weight = quick * options.getTriangleTimeFactor() + slope
                        * options.getTriangleSlopeFactor() + safety
                        * options.getTriangleSafetyFactor();
                weight /= speed;
                break;
            default:
                weight = length / speed;
            }
        } else {
            weight = time;
        }
        if (isStairs()) {
            weight *= options.stairsReluctance;
        } else {
            weight *= options.walkReluctance;
        }
        
        StateEditor s1 = s0.edit(this);
        s1.setBackMode(traverseMode);

        if (wheelchairNotes != null && options.wheelchairAccessible) {
            s1.addAlerts(wheelchairNotes);
        }

        PlainStreetEdge backPSE;
        if (backEdge != null && backEdge instanceof PlainStreetEdge) {
            backPSE = (PlainStreetEdge) backEdge;
            int outAngle = 0;
            int inAngle = 0;
            if (options.arriveBy) {
                if (!canTurnOnto(backPSE, s0, traverseMode))
                    return null;
                outAngle = backPSE.getOutAngle();
                inAngle = getInAngle();
            } else {
                if (!backPSE.canTurnOnto(this, s0, traverseMode))
                    return null;
                outAngle = getOutAngle();
                inAngle = backPSE.getInAngle();
            }

            int turnCost = Math.abs(outAngle - inAngle);
            if (turnCost > 180) {
                turnCost = 360 - turnCost;
            }
            final double realTurnCost = (turnCost / 20.0) / speed;
            s1.incrementWalkDistance(realTurnCost / 100); //just a tie-breaker
            weight += realTurnCost;
            long turnTime = (long) realTurnCost;
            if (turnTime != realTurnCost) {
                turnTime++;
            }
            time += turnTime;
        }
        s1.incrementWalkDistance(length);
        int timeLong = (int) time;
        if (timeLong != time) {
            timeLong++;
        }
        s1.incrementTimeInSeconds(timeLong);
        s1.incrementWeight(weight);
        if (s1.weHaveWalkedTooFar(options))
            return null;
        
        s1.addAlerts(getNotes());

        return s1.makeState();
    }

    @Override
    public double weightLowerBound(RoutingRequest options) {
        return timeLowerBound(options) * options.walkReluctance;
    }

    @Override
    public double timeLowerBound(RoutingRequest options) {
        return this.length / options.getSpeedUpperBound();
    }

    public void setSlopeSpeedEffectiveLength(double slopeSpeedEffectiveLength) {
        elevationProfileSegment.setSlopeSpeedEffectiveLength(slopeSpeedEffectiveLength);
    }

    public double getSlopeSpeedEffectiveLength() {
        return elevationProfileSegment.getSlopeSpeedEffectiveLength();
    }

    public void setSlopeWorkCost(double slopeWorkCost) {
        elevationProfileSegment.setSlopeWorkCost(slopeWorkCost);
    }

    public double getWorkCost() {
        return elevationProfileSegment.getSlopeWorkCost();
    }

    public void setBicycleSafetyEffectiveLength(double bicycleSafetyEffectiveLength) {
        elevationProfileSegment.setBicycleSafetyEffectiveLength(bicycleSafetyEffectiveLength);
    }

    public double getBicycleSafetyEffectiveLength() {
        return elevationProfileSegment.getBicycleSafetyEffectiveLength();
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
        id = null; 
        out.defaultWriteObject();
    }

    public boolean getSlopeOverride() {
        return elevationProfileSegment.getSlopeOverride();
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public PackedCoordinateSequence getElevationProfile(double start, double end) {
        return elevationProfileSegment.getElevationProfile(start, end);
    }

    public void setSlopeOverride(boolean slopeOverride) {
        elevationProfileSegment.setSlopeOverride(slopeOverride);
    }

    public void setRoundabout(boolean roundabout) {
        this.roundabout = roundabout;
    }

    public boolean isRoundabout() {
        return roundabout;
    }

    public Set<Alert> getNotes() {
    	return notes;
    }
    
    public void setNote(Set<Alert> notes) {
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

    public boolean isStairs() {
        return stairs;
    }

    public void setStairs(boolean stairs) {
        this.stairs = stairs;
    }

    public void setName(String name) {
       this.name = name;
    }

    public void setWheelchairNote(Set<Alert> wheelchairNotes) {
        this.wheelchairNotes = wheelchairNotes;
    }

    public Set<Alert> getWheelchairNotes() {
        return wheelchairNotes;
    }

    public int getStreetClass() {
        return streetClass;
    }

    public void setStreetClass(int streetClass) {
        this.streetClass = streetClass;
    }

    public void addTurnRestriction(TurnRestriction turnRestriction) {
        if (turnRestrictions.isEmpty()) {
            turnRestrictions = new ArrayList<TurnRestriction>();
        }
        turnRestrictions.add(turnRestriction);
    }

    @Override
    public PlainStreetEdge clone() {
        try {
            return (PlainStreetEdge) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<TurnRestriction> getTurnRestrictions() {
        return turnRestrictions;
    }

    public boolean canTurnOnto(Edge e, State state, TraverseMode mode) {
        for (TurnRestriction restriction : turnRestrictions) {
            /* FIXME: This is wrong for trips that end in the middle of restriction.to
             */

            if (restriction.type == TurnRestrictionType.ONLY_TURN) {
                if (restriction.to != e && restriction.modes.contains(mode)) {
                    return false;
                }
            } else {
                if (restriction.to == e && restriction.modes.contains(mode)) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getInAngle() {
        return inAngle;
    }

    public int getOutAngle() {
        return outAngle;
    }

    @Override
    public ElevationProfileSegment getElevationProfileSegment() {
        return elevationProfileSegment;
    }
}
