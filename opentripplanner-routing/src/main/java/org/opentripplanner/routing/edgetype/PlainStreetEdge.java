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

import lombok.Getter;
import lombok.Setter;

import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.TurnRestrictionType;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.util.ElevationProfileSegment;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * This represents a street segment.
 * 
 * @author novalis
 * 
 */
public class PlainStreetEdge extends StreetEdge implements Cloneable {

    private static Logger LOG = LoggerFactory.getLogger(PlainStreetEdge.class); 

    private static final long serialVersionUID = 1L;

    private static final double GREENWAY_SAFETY_FACTOR = 0.1;

    private ElevationProfileSegment elevationProfileSegment;

    private double length;

    private LineString geometry;
    
    private String name;

    private boolean wheelchairAccessible = true;

    private StreetTraversalPermission permission;

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
    
    /** The speed in meters per second that an automobile can traverse this street segment at */
    private float carSpeed;
    
    /** This street has a toll */
    private boolean toll;

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

    public PlainStreetEdge(StreetVertex v1, StreetVertex v2, LineString geometry, 
            String name, double length,
            StreetTraversalPermission permission, boolean back) {
        // use a default car speed of ~25 mph for splitter vertices and the like
        this(v1, v2, geometry, name, length, permission, back, 11.2f);
    }

    public PlainStreetEdge(StreetVertex v1, StreetVertex v2, LineString geometry, 
            String name, double length,
            StreetTraversalPermission permission, boolean back, float carSpeed) {
        super(v1, v2);
        this.geometry = geometry;
        this.length = length;
        this.elevationProfileSegment = new ElevationProfileSegment(length);
        this.name = name;
        this.permission = permission;
        this.back = back;
        this.carSpeed = carSpeed;
        if (geometry != null) {
            try {
                for (Coordinate c : geometry.getCoordinates()) {
                    if (Double.isNaN(c.x)) {
                        System.out.println("X DOOM");
                    }
                    if (Double.isNaN(c.y)) {
                        System.out.println("Y DOOM");
                    }
                }
                double angleR = DirectionUtils.getLastAngle(geometry);
                outAngle = ((int) (180 * angleR / Math.PI) + 180 + 360) % 360;
                angleR = DirectionUtils.getFirstAngle(geometry);
                inAngle = ((int) (180 * angleR / Math.PI) + 180 + 360) % 360;
            } catch (IllegalArgumentException iae) {
                LOG.error("exception while determining street edge angles. setting to zero. there is probably something wrong with this street segment's geometry.");
                inAngle = 0;
                outAngle = 0;
            }
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
        return doTraverse(s0, options, s0.getNonTransitMode());
    }

    private State doTraverse(State s0, RoutingRequest options, TraverseMode traverseMode) {
		Edge backEdge = s0.getBackEdge();
		if (backEdge != null
				&& (options.arriveBy ? (backEdge.getToVertex() == fromv)
						: (backEdge.getFromVertex() == tov))) {
			// no illegal U-turns
			return null;
		}
		if (!canTraverse(options, traverseMode)) {
			if (traverseMode == TraverseMode.BICYCLE) {
				// try walking bike since you can't ride here
				return doTraverse(s0, options.getWalkingOptions(),
						TraverseMode.WALK);
			}
			return null;
		}

		double speed;
        
        // Automobiles have variable speeds depending on the edge type
        if (traverseMode == TraverseMode.CAR)
            speed = this.calculateCarSpeed(options);
        else
            speed = options.getSpeed(traverseMode);
         
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
            if (options.isWalkingBike()) {
                //take slopes into account when walking bikes
                time = elevationProfileSegment.getSlopeSpeedEffectiveLength() / speed;
            }
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
            float backSpeed = (float) (traverseMode == TraverseMode.CAR ? backPSE.getCarSpeed() :
                options.getSpeed(traverseMode));
            
            final double realTurnCost;
            
            /* Compute turn cost.
             * 
             * This is a subtle piece of code. Turn costs are evaluated differently during
             * forward and reverse traversal. During forward traversal of an edge, the turn
             * *into* that edge is used, while during reverse traversal, the turn *out of*
             * the edge is used.
             *
             * However, over a set of edges, the turn costs must add up the same (for
             * general correctness and specifically for reverse optimization). This means
             * that during reverse traversal, we must also use the speed for the mode of
             * the backEdge, rather than of the current edge.
             */
            if (options.arriveBy && tov instanceof IntersectionVertex) { // arrive-by search
                if (!canTurnOnto(backPSE, s0, traverseMode))
                    return null;
                realTurnCost = ((IntersectionVertex) tov).computeTraversalCost(
                        this, backPSE, traverseMode, options, (float) speed, backSpeed);
            } else if (fromv instanceof IntersectionVertex) { // depart-after search
                if (!backPSE.canTurnOnto(this, s0, traverseMode))
                    return null;
                realTurnCost = ((IntersectionVertex) fromv).computeTraversalCost(
                        backPSE, this, traverseMode, options, backSpeed, (float) speed);
            } else { // in case this is a temporary edge not connected to an IntersectionVertex
                realTurnCost = 0; 
            }
                       
            if (traverseMode != TraverseMode.CAR) 
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
        
        if (traverseMode != TraverseMode.CAR)
            s1.incrementWalkDistance(length);

        s1.incrementWeight(weight);
        if (s1.weHaveWalkedTooFar(options))
            return null;
        
        s1.addAlerts(notes);
        
        if (this.toll && traverseMode == TraverseMode.CAR)
            s1.addAlert(Alert.createSimpleAlerts("Toll road"));

        return s1.makeState();
    }

	/**
	 * Calculate the average automobile traversal speed of this segment, given
	 * the RoutingRequest, and return it in meters per second.
	 * 
	 * @param options
	 * @return
	 */
	private double calculateCarSpeed(RoutingRequest options) {
		return this.carSpeed;
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

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
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
        return "PlainStreetEdge(" + name + ", " + fromv + " -> " + tov + ")";
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
                if (restriction.to != e && restriction.modes.contains(mode) &&
                        restriction.active(state.getTime())) {
                    return false;
                }
            } else {
                if (restriction.to == e && restriction.modes.contains(mode) &&
                        restriction.active(state.getTime())) {
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

    public void setCarSpeed(float carSpeed) {
        this.carSpeed = carSpeed;         
    }
    
    public float getCarSpeed() {
        return carSpeed;
    }
    
    public void setToll(boolean toll) {
        this.toll = toll;
    }
    
    public boolean getToll() {
        return this.toll;
    }

    protected boolean detachFrom() {
        for (Edge e : fromv.getIncoming()) {
            if (!(e instanceof PlainStreetEdge)) continue;
            PlainStreetEdge pse = (PlainStreetEdge) e;
            ArrayList<TurnRestriction> restrictions = new ArrayList<TurnRestriction>(pse.turnRestrictions);
            for (TurnRestriction restriction : restrictions) {
                if (restriction.to == this) {
                    pse.turnRestrictions.remove(restriction);
                }
            }
        }
        return super.detachFrom();
    }

}
