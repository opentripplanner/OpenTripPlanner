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
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.util.ElevationProfileSegment;
import org.opentripplanner.routing.util.ElevationUtils;
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

    @Getter
    private double length;

    @Getter
    private LineString geometry;
    
    @Getter @Setter
    private String name;

    @Getter @Setter
    private String label;
    
    @Getter @Setter
    private boolean wheelchairAccessible = true;

    @Getter @Setter
    private StreetTraversalPermission permission;

    @Getter @Setter
    private int streetClass = CLASS_OTHERPATH;
    
    /**
     * Marks that this edge is the reverse of the one defined in the source
     * data. Does NOT mean fromv/tov are reversed.
     */
    @Getter @Setter
    public boolean back;
    
    @Getter @Setter
    private boolean roundabout = false;
    
    @Getter
    private Set<Alert> notes;

    @Setter
    private boolean hasBogusName;

    @Getter @Setter
    private boolean noThruTraffic;

    /**
     * This street is a staircase
     */
    @Getter @Setter
    private boolean stairs;
    
    /**
     * The speed (meters / sec) at which an automobile can traverse
     * this street segment.
     */
    @Getter @Setter
    private float carSpeed;
    
    /** This street has a toll */
    @Getter @Setter
    private boolean toll;

    @Getter
    private Set<Alert> wheelchairNotes;

    @Getter
    private List<TurnRestriction> turnRestrictions = Collections.emptyList();

    /** 0 -> 360 degree angle - the angle at the start of the edge geometry */
    @Getter
    public int inAngle;

    /** 0 -> 360 degree angle - the angle at the end of the edge geometry */
    @Getter
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
        // TODO(flamholz): do something smarter with the car speed here.
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
        
        return canTraverse(options.getModes());
    }
    
    @Override
    public boolean canTraverse(TraverseModeSet modes) {
        return permission.allows(modes);
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
        return permission.allows(mode);
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
    public State traverse(State s0) {
        final RoutingRequest options = s0.getOptions();
        return doTraverse(s0, options, s0.getNonTransitMode());
    }

    private State doTraverse(State s0, RoutingRequest options, TraverseMode traverseMode) {
        Edge backEdge = s0.getBackEdge();
        if (backEdge != null) {
            // No illegal U-turns.
            // NOTE(flamholz): we check both directions because both edges get a chance to decide
            // if they are the reverse of the other. Also, because it doesn't matter which direction
            // we are searching in - these traversals are always disallowed (they are U-turns in one direction
            // or the other).
            if (this.isReverseOf(backEdge) || backEdge.isReverseOf(this)) {
                return null;
            }
        }

        if (!canTraverse(options, traverseMode)) {
            if (traverseMode == TraverseMode.BICYCLE) {
                // try walking bike since you can't ride here
                return doTraverse(s0, options.getBikeWalkingOptions(),
                        TraverseMode.WALK);
            }
            return null;
        }

        // Automobiles have variable speeds depending on the edge type
        double speed = calculateSpeed(options, traverseMode);
        
        double time = length / speed;
        double weight;
        // TODO(flamholz): factor out this bike, wheelchair and walking specific logic to somewhere central.
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
                // take slopes into account when walking bikes
                time = elevationProfileSegment.getSlopeSpeedEffectiveLength() / speed;
            }
            weight = time;
            if (traverseMode.equals(TraverseMode.WALK)) {
                // take slopes into account when walking
                double costs = ElevationUtils.getWalkCostsForSlope(length, elevationProfileSegment.getMaxSlope());
                // as the cost walkspeed is assumed to be for 4.8km/h (= 1.333 m/sec) we need to adjust
                // for the walkspeed set by the user
                weight = costs * ( 1.3333 / speed );
                time = weight; //treat cost as time, as in the current model it actually is the same (this can be checked for maxSlope == 0)
                /*
                // debug code
                if(weight > 100){
                    double timeflat = length / speed;
                    System.out.format("line length: %.1f m, slope: %.3f ---> slope costs: %.1f , weight: %.1f , time (flat):  %.1f %n", length, elevationProfileSegment.getMaxSlope(), costs, weight, timeflat);
                }
                */
            }
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
            double backSpeed = backPSE.calculateSpeed(options, traverseMode);
            final double realTurnCost;  // Units are seconds.
            
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
                if (!canTurnOnto(backPSE, s0, traverseMode)) {
                    return null;
                }
                IntersectionVertex traversedVertex = ((IntersectionVertex) tov);
                realTurnCost = options.getIntersectionTraversalCostModel().computeTraversalCost(
                        traversedVertex, this, backPSE, traverseMode, options, (float) speed,
                        (float) backSpeed);
            } else if (fromv instanceof IntersectionVertex) { // depart-after search
                if (!backPSE.canTurnOnto(this, s0, traverseMode)) {
                    return null;
                }
                IntersectionVertex traversedVertex = ((IntersectionVertex) fromv);
                realTurnCost = options.getIntersectionTraversalCostModel().computeTraversalCost(
                        traversedVertex, backPSE, this, traverseMode, options, (float) backSpeed,
                        (float) speed);                
            } else { // in case this is a temporary edge not connected to an IntersectionVertex
                LOG.info("Not computing turn cost for edge {}", this);
                realTurnCost = 0; 
            }

            if (!traverseMode.isDriving()) {
                s1.incrementWalkDistance(realTurnCost / 100);  // just a tie-breaker
            }

            long turnTime = (long) Math.ceil(realTurnCost);
            time += turnTime;
            weight += options.turnReluctance * realTurnCost;
        }

        int timeLong = (int) Math.ceil(time);
        s1.incrementTimeInSeconds(timeLong);
        
        s1.incrementWeight(weight);
        if (!traverseMode.isDriving()) {
            s1.incrementWalkDistance(length);
        }
        
        if (s1.weHaveWalkedTooFar(options)) {
            LOG.debug("Too much walking. Bailing.");
            return null;
        }
        
        s1.addAlerts(notes);
        
        if (this.isToll() && traverseMode.isDriving()) {
            s1.addAlert(Alert.createSimpleAlerts("Toll road"));
        }
        
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
        return carSpeed;
    }
    
    /**
     * Calculate the speed appropriately given the RoutingRequest and traverseMode.
     * 
     * @param options
     * @param traverseMode
     * @return
     */
    private double calculateSpeed(RoutingRequest options, TraverseMode traverseMode) {
        if (traverseMode == TraverseMode.CAR) {
            // NOTE: Automobiles have variable speeds depending on the edge type
            return calculateCarSpeed(options);
        }
        return options.getSpeed(traverseMode);
    }

    @Override
    public double weightLowerBound(RoutingRequest options) {
        return timeLowerBound(options) * options.walkReluctance;
    }

    @Override
    public double timeLowerBound(RoutingRequest options) {
        return this.length / options.getStreetSpeedUpperBound();
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
    
    public void setNote(Set<Alert> notes) {
        this.notes = notes;
    }
    
    @Override
    public String toString() {
        return "PlainStreetEdge(" + getId() + ", " + name + ", " + fromv + " -> " + tov
                + " length=" + this.getLength() + " carSpeed=" + this.getCarSpeed() + ")";
    }

    public boolean hasBogusName() {
        return hasBogusName;
    }

    /** Returns true if there are any turn restrictions defined. */
    public boolean hasExplicitTurnRestrictions() {
        return this.turnRestrictions != null && this.turnRestrictions.size() > 0;
    }

    public void setWheelchairNote(Set<Alert> wheelchairNotes) {
        this.wheelchairNotes = wheelchairNotes;
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
    
    public boolean canTurnOnto(Edge e, State state, TraverseMode mode) {
        for (TurnRestriction restriction : turnRestrictions) {
            /* FIXME: This is wrong for trips that end in the middle of restriction.to
             */

            // NOTE(flamholz): edge to be traversed decides equivalence. This is important since 
            // it might be a temporary edge that is equivalent to some graph edge.
            if (restriction.type == TurnRestrictionType.ONLY_TURN) {
                if (!e.isEquivalentTo(restriction.to) && restriction.modes.contains(mode) &&
                        restriction.active(state.getTime())) {
                    return false;
                }
            } else {
                if (e.isEquivalentTo(restriction.to) && restriction.modes.contains(mode) &&
                        restriction.active(state.getTime())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ElevationProfileSegment getElevationProfileSegment() {
        return elevationProfileSegment;
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
