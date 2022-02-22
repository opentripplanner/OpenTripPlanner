package org.opentripplanner.routing.edgetype;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.TurnRestrictionType;
import org.opentripplanner.common.geometry.CompactLineString;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.linking.DisposableEdgeCollection;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.BarrierVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.BitSetUtils;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This represents a street segment.
 * 
 * @author novalis
 * 
 */
public class StreetEdge extends Edge implements BikeWalkableEdge, Cloneable, CarPickupableEdge {

    private static final Logger LOG = LoggerFactory.getLogger(StreetEdge.class);

    private StreetEdgeCostExtension costExtension;

    private static final long serialVersionUID = 1L;

    /* TODO combine these with OSM highway= flags? */
    public static final int CLASS_STREET = 3;
    public static final int CLASS_CROSSING = 4;
    public static final int CLASS_OTHERPATH = 5;
    public static final int CLASS_OTHER_PLATFORM = 8;
    public static final int CLASS_TRAIN_PLATFORM = 16;
    public static final int CLASS_LINK = 32; // on/offramps; OSM calls them "links"

    private static final double GREENWAY_SAFETY_FACTOR = 0.1;

    // TODO(flamholz): do something smarter with the car speed here.
    public static final float DEFAULT_CAR_SPEED = 11.2f;

    /** If you have more than 8 flags, increase flags to short or int */
    private static final int BACK_FLAG_INDEX = 0;
    private static final int ROUNDABOUT_FLAG_INDEX = 1;
    private static final int HASBOGUSNAME_FLAG_INDEX = 2;
    private static final int MOTOR_VEHICLE_NOTHRUTRAFFIC = 3;
    private static final int STAIRS_FLAG_INDEX = 4;
    private static final int SLOPEOVERRIDE_FLAG_INDEX = 5;
    private static final int WHEELCHAIR_ACCESSIBLE_FLAG_INDEX = 6;
    private static final int BICYCLE_NOTHRUTRAFFIC = 7;
    private static final int WALK_NOTHRUTRAFFIC = 8;

    /** back, roundabout, stairs, ... */
    private short flags;

    /**
     * Length is stored internally as 32-bit fixed-point (millimeters). This allows edges of up to ~2100km.
     * Distances used in calculations and exposed outside this class are still in double-precision floating point meters.
     * Someday we might want to convert everything to fixed point representations.
     */
    private int length_mm;

    /**
     * bicycleSafetyWeight = length * bicycleSafetyFactor. For example, a 100m street with a safety
     * factor of 2.0 will be considered in term of safety cost as the same as a 150m street with a
     * safety factor of 1.0.
     */
    protected float bicycleSafetyFactor;

    private byte[] compactGeometry;
    
    private I18NString name;

    private StreetTraversalPermission permission;

    /** The OSM way ID from whence this came - needed to reference traffic data */
    public long wayId;

    private int streetClass = CLASS_OTHERPATH;
    
    /**
     * The speed (meters / sec) at which an automobile can traverse
     * this street segment.
     */
    private float carSpeed;

    /**
     * The angle at the start of the edge geometry.
     * Internal representation is -180 to +179 integer degrees mapped to -128 to +127 (brads)
     */
    private byte inAngle;

    /** The angle at the start of the edge geometry. Internal representation like that of inAngle. */
    private byte outAngle;

    /**
     * The set of turn restrictions of this edge. Since most instances don't have any, we reuse
     * a global instance in order to conserve memory.
     *
     * This field is optimized for low memory consumption and fast access, but modification is
     * synchronized since it can happen concurrently.
     *
     * Why not use null to represent no turn restrictions?
     * This would mean that the access would also need to be synchronized but since that is a very
     * hot code path, it needs to be fast.
     *
     * Why not use a concurrent collection?
     * That would mean that every StreetEdge has its own empty instance which would increase
     * memory significantly.
     */
    private List<TurnRestriction> turnRestrictions = List.of();

    public StreetEdge(StreetVertex v1, StreetVertex v2, LineString geometry,
                      I18NString name, double length,
                      StreetTraversalPermission permission, boolean back) {
        super(v1, v2);
        this.setBack(back);
        this.setGeometry(geometry);
        this.length_mm = (int) (length * 1000); // CONVERT FROM FLOAT METERS TO FIXED MILLIMETERS
        if (this.length_mm == 0) {
            LOG.warn("StreetEdge {} from {} to {} has length of 0. This is usually an error.", name, v1, v2);
        }
        this.bicycleSafetyFactor = 1.0f;
        this.name = name;
        this.setPermission(permission);
        this.setCarSpeed(DEFAULT_CAR_SPEED);
        this.setWheelchairAccessible(true); // accessible by default
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
                // Conversion from radians to internal representation as a single signed byte.
                // We also reorient the angles since OTP seems to use South as a reference
                // while the azimuth functions use North.
                // FIXME Use only North as a reference, not a mix of North and South!
                // Range restriction happens automatically due to Java signed overflow behavior.
                // 180 degrees exists as a negative rather than a positive due to the integer range.
                double angleRadians = DirectionUtils.getLastAngle(geometry);
                outAngle = (byte) Math.round(angleRadians * 128 / Math.PI + 128);
                angleRadians = DirectionUtils.getFirstAngle(geometry);
                inAngle = (byte) Math.round(angleRadians * 128 / Math.PI + 128);
            } catch (IllegalArgumentException iae) {
                LOG.error("exception while determining street edge angles. setting to zero. there is probably something wrong with this street segment's geometry.");
                inAngle = 0;
                outAngle = 0;
            }
        }
    }


    //For testing only
    public StreetEdge(StreetVertex v1, StreetVertex v2, LineString geometry,
                      String name, double length,
                      StreetTraversalPermission permission, boolean back) {
        this(v1, v2, geometry, new NonLocalizedString(name), length, permission, back);
    }

    public StreetEdge(StreetVertex v1, StreetVertex v2, LineString geometry, I18NString name,
            StreetTraversalPermission permission, boolean back
    ) {
        this(v1, v2, geometry, name, SphericalDistanceLibrary.length(geometry), permission, back);
    }

    /**
     * Checks permissions of the street edge if specified modes are allowed to travel.
     *
     * Barriers aren't taken into account. So it can happen that canTraverse returns True.
     * But doTraverse returns false. Since there are barriers on a street.
     *
     * This is because this function is used also on street when searching for start/stop.
     * Those streets are then split. On splitted streets can be possible to drive with a CAR because
     * it is only blocked from one way.
     * @param modes
     * @return
     */
    public boolean canTraverse(TraverseModeSet modes) {
        return getPermission().allows(modes);
    }

    /**
     * Checks if edge is accessible for wheelchair if needed according to tags or if slope is too big.
     *
     * Then it checks if street can be traversed according to street permissions and start/end barriers.
     * This is done with intersection of street and barrier permissions in {@link #canTraverseIncludingBarrier(TraverseMode)}
     *
     * @param options
     * @param mode
     * @return
     */
    private boolean canTraverse(RoutingRequest options, TraverseMode mode) {
        if (mode.isWalking() && options.wheelchairAccessible) {
            if (!isWheelchairAccessible()) {
                return false;
            }
            if (getMaxSlope() > options.maxWheelchairSlope) {
                return false;
            }
        }
        return canTraverseIncludingBarrier(mode);
    }

    /**
     * This checks if start or end vertex is bollard
     * If it is it creates intersection of street edge permissions
     * and from/to barriers.
     * Then it checks if mode is allowed to traverse the edge.
     *
     * By default CAR isn't allowed to traverse barrier but foot and bicycle are.
     * This can be changed with different tags
     *
     * If start/end isn't bollard it just checks the street permissions.
     *
     * It is used in {@link #canTraverse(RoutingRequest, TraverseMode)}
     * @param mode
     * @return
     */
    public boolean canTraverseIncludingBarrier(TraverseMode mode) {
        StreetTraversalPermission permission = getPermission();
        if (fromv instanceof BarrierVertex) {
            permission = permission.intersection(((BarrierVertex) fromv).getBarrierPermissions());
        }
        if (tov instanceof BarrierVertex) {
            permission = permission.intersection(((BarrierVertex) tov).getBarrierPermissions());
        }

        return permission.allows(mode);
    }

    public PackedCoordinateSequence getElevationProfile() {
        return null;
    }

    public boolean isElevationFlattened() {
        return false;
    }

    public float getMaxSlope() {
        return 0.0f;
    }

    @Override
    public double getDistanceMeters() {
        return length_mm / 1000.0;
    }

    @Override
    public State traverse(State s0) {
        final RoutingRequest options = s0.getOptions();
        final StateEditor editor;

        // If we are biking, or walking with a bike check if we may continue by biking or by walking
        if (s0.getNonTransitMode() == TraverseMode.BICYCLE) {
            if (canTraverse(options, TraverseMode.BICYCLE)) {
                editor = doTraverse(s0, options, TraverseMode.BICYCLE, false);
            } else if (canTraverse(options, TraverseMode.WALK)) {
                editor = doTraverse(s0, options, TraverseMode.WALK, true);
            } else {
                return null;
            }
        } else if (canTraverse(options, s0.getNonTransitMode())) {
            editor = doTraverse(s0, options, s0.getNonTransitMode(), false);
        } else {
            editor = null;
        }

        State state = editor != null ? editor.makeState() : null;

        if (canPickupAndDrive(s0)) {
            StateEditor inCar = doTraverse(s0, options, TraverseMode.CAR, false);
            if (inCar != null) {
                driveAfterPickup(s0, inCar);
                State forkState = inCar.makeState();
                if (forkState != null) {
                    // Return both the original WALK state, along with the new IN_CAR state
                    forkState.addToExistingResultChain(state);
                    return forkState;
                }
            }
        }

        if (canDropOffAfterDriving(s0) && !getPermission().allows(TraverseMode.CAR)) {
            StateEditor dropOff = doTraverse(s0, options, TraverseMode.WALK, false);
            if (dropOff != null) {
                dropOffAfterDriving(s0, dropOff);
                // Only the walk state is returned, since traversing by car was not possible
                return dropOff.makeState();
            }
        }

        return state;
    }

    /** return a StateEditor rather than a State so that we can make parking/mode switch modifications for kiss-and-ride. */
    private StateEditor doTraverse(
            State s0,
            RoutingRequest options,
            TraverseMode traverseMode,
            boolean walkingBike
    ) {
        if (traverseMode == null) { return null; }
        boolean backWalkingBike = s0.isBackWalkingBike();
        TraverseMode backMode = s0.getBackMode();
        Edge backEdge = s0.getBackEdge();
        if (backEdge != null) {
            // No illegal U-turns.
            // NOTE(flamholz): we check both directions because both edges get a chance to decide
            // if they are the reverse of the other. Also, because it doesn't matter which direction
            // we are searching in - these traversals are always disallowed (they are U-turns in one direction
            // or the other).
            // TODO profiling indicates that this is a hot spot.
            if (this.isReverseOf(backEdge) || backEdge.isReverseOf(this)) {
                return null;
            }
        }

        /* Check whether this street allows the current mode. */
        if (!canTraverse(options, traverseMode)) {
            return null;
        }

        // Automobiles have variable speeds depending on the edge type
        double speed = calculateSpeed(options, traverseMode, walkingBike);

        double time;
        double weight;
        // TODO(flamholz): factor out this bike, wheelchair and walking specific logic to somewhere central.
        switch (traverseMode) {
            case BICYCLE:
                time = getEffectiveBikeDistance() / speed;
                switch (options.bicycleOptimizeType) {
                    case SAFE:
                        weight = bicycleSafetyFactor * getDistanceMeters() / speed;
                        break;
                    case GREENWAYS:
                        weight = bicycleSafetyFactor * getDistanceMeters() / speed;
                        if (bicycleSafetyFactor <= GREENWAY_SAFETY_FACTOR) {
                            // greenways are treated as even safer than they really are
                            weight *= 0.66;
                        }
                        break;
                    case FLAT:
                        /* see notes in StreetVertex on speed overhead */
                        weight = getDistanceMeters() / speed + getEffectiveBikeWorkCost();
                        break;
                    case QUICK:
                        weight = getEffectiveBikeDistance() / speed;
                        break;
                    case TRIANGLE:
                        double quick = getEffectiveBikeDistance();
                        double safety = bicycleSafetyFactor * getDistanceMeters();
                        // TODO This computation is not coherent with the one for FLAT
                        double slope = getEffectiveBikeWorkCost();
                        weight = quick * options.bikeTriangleTimeFactor + slope
                                * options.bikeTriangleSlopeFactor + safety
                                * options.bikeTriangleSafetyFactor;
                        weight /= speed;
                        break;
                    default:
                        weight = getDistanceMeters() / speed;
                }
                break;
            case WALK:
                if (options.wheelchairAccessible) {
                    time = getEffectiveWalkDistance() / speed;
                    weight = getEffectiveBikeDistance() / speed;
                } else if (walkingBike) {
                    // take slopes into account when walking bikes
                    time = weight = getEffectiveBikeDistance() / speed;
                } else {
                    // take slopes into account when walking
                    time = weight = getEffectiveWalkDistance() / speed;
                }
                break;
            default:
                time = weight = getDistanceMeters() / speed;
        }

        if (isStairs()) {
            weight *= options.stairsReluctance;
        } else {
            weight *= options.getReluctance(traverseMode, walkingBike);
        }

        var s1 = createEditor(s0, this, traverseMode, walkingBike);

        if (isTraversalBlockedByNoThruTraffic(traverseMode, backEdge, s0, s1)) {
            return null;
        }

        int roundedTime = (int) Math.ceil(time);

        /* Compute turn cost. */
        StreetEdge backPSE;
        if (backEdge instanceof StreetEdge) {
            backPSE = (StreetEdge) backEdge;
            RoutingRequest backOptions = s0.getOptions();
            double backSpeed = backPSE.calculateSpeed(backOptions, backMode, backWalkingBike);
            final double realTurnCost;  // Units are seconds.

            // Apply turn restrictions
            if (options.arriveBy && !canTurnOnto(backPSE, s0, backMode)) {
                return null;
            } else if (!options.arriveBy && !backPSE.canTurnOnto(this, s0, traverseMode)) {
                return null;
            }

            /*
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
                IntersectionVertex traversedVertex = ((IntersectionVertex) tov);

                realTurnCost = backOptions.getIntersectionTraversalCostModel().computeTraversalCost(
                        traversedVertex, this, backPSE, backMode, backOptions, (float) speed,
                        (float) backSpeed);
            } else if (!options.arriveBy && fromv instanceof IntersectionVertex) { // depart-after search
                IntersectionVertex traversedVertex = ((IntersectionVertex) fromv);

                realTurnCost = options.getIntersectionTraversalCostModel().computeTraversalCost(
                        traversedVertex, backPSE, this, traverseMode, options, (float) backSpeed,
                        (float) speed);
            } else {
                // In case this is a temporary edge not connected to an IntersectionVertex
                LOG.debug("Not computing turn cost for edge {}", this);
                realTurnCost = 0;
            }

            if (!traverseMode.isDriving()) {
                s1.incrementWalkDistance(realTurnCost / 100);  // just a tie-breaker
            }

            int turnTime = (int) Math.ceil(realTurnCost);
            roundedTime += turnTime;
            weight += options.turnReluctance * realTurnCost;
        }

        if (!traverseMode.isDriving()) {
            s1.incrementWalkDistance(getEffectiveBikeDistance());
        }

        if (costExtension != null) {
           weight += costExtension.calculateExtraCost(options, length_mm, traverseMode);
        }

        s1.incrementTimeInSeconds(roundedTime);

        s1.incrementWeight(weight);

        return s1;
    }

    /* The no-thru traffic support works by not allowing a transition from a no-thru area out of it.
     * It allows starting in a no-thru area by checking for a transition from a "normal"
     * (thru-traffic allowed) edge to a no-thru edge. Once a transition is recorded
     * (State#hasEnteredNoThruTrafficArea), traverseing "normal" edges is blocked.
     *
     * Since a Vertex may be arrived at with and without a no-thru restriction, the logic in
     * DominanceFunction#betterOrEqualAndComparable treats the two cases as separate.
     */
    private boolean isTraversalBlockedByNoThruTraffic(
            TraverseMode traverseMode,
            Edge backEdge,
            State s0,
            StateEditor s1
    ) {
        if (isNoThruTraffic(traverseMode)) {
            // Record transition into no-through-traffic area.
            if (backEdge instanceof StreetEdge
                    && !((StreetEdge) backEdge).isNoThruTraffic(traverseMode)) {
                s1.setEnteredNoThroughTrafficArea();
            }
        } else if (s0.hasEnteredNoThruTrafficArea()) {
            // If we transitioned into a no-through-traffic area at some point, check if we are exiting it.
            return true;
        }

        return false;
    }

    public boolean isNoThruTraffic(TraverseMode traverseMode) {
        if (traverseMode.isCycling()) {
            return isBicycleNoThruTraffic();
        }

        if (traverseMode.isDriving()) {
            return isMotorVehicleNoThruTraffic();
        }

        if (traverseMode.isWalking()) {
            return isWalkNoThruTraffic();
        }

        return false;
    }

    /**
     * Calculate the average automobile traversal speed of this segment, given
     * the RoutingRequest, and return it in meters per second.
     */
    private double calculateCarSpeed(RoutingRequest options) {
        return getCarSpeed();
    }
    
    /**
     * Calculate the speed appropriately given the RoutingRequest and traverseMode.
     */
    public double calculateSpeed(
            RoutingRequest options,
            TraverseMode traverseMode,
            boolean walkingBike
    ) {
        if (traverseMode == null) {
            return Double.NaN;
        } else if (traverseMode.isDriving()) {
            // NOTE: Automobiles have variable speeds depending on the edge type
            return calculateCarSpeed(options);
        }
        final double speed = options.getSpeed(traverseMode, walkingBike);
        return isStairs() ? (speed / options.stairsTimeFactor) : speed;
    }

    /**
     * This gets the effective length for bikes and wheelchairs, taking slopes into account. This
     * can be divided by the speed on a flat surface to get the duration.
     */
    public double getEffectiveBikeDistance() {
        return getDistanceMeters();
    }

    /**
     * This gets the effective work amount for bikes, taking the effort required to traverse the
     * slopes into account.
     */
    public double getEffectiveBikeWorkCost() {
        return getDistanceMeters();
    }

    @Override
    public double getEffectiveWalkDistance() {
        return getDistanceMeters();
    }

    public void setBicycleSafetyFactor(float bicycleSafetyFactor) {
        this.bicycleSafetyFactor = bicycleSafetyFactor;
    }

    public float getBicycleSafetyFactor() {
        return bicycleSafetyFactor;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    public String toString() {
        return "StreetEdge(" + name + ", " + fromv + " -> " + tov
                + " length=" + this.getDistanceMeters() + " carSpeed=" + this.getCarSpeed()
                + " permission=" + this.getPermission() + ")";
    }

    @Override
    public StreetEdge clone() {
        try {
            return (StreetEdge) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public boolean canTurnOnto(Edge e, State state, TraverseMode mode) {
        for (TurnRestriction turnRestriction : turnRestrictions) {
            /* FIXME: This is wrong for trips that end in the middle of turnRestriction.to
             */

            // NOTE(flamholz): edge to be traversed decides equivalence. This is important since 
            // it might be a temporary edge that is equivalent to some graph edge.
            if (turnRestriction.type == TurnRestrictionType.ONLY_TURN) {
                if (!e.isEquivalentTo(turnRestriction.to) && turnRestriction.modes.contains(mode) &&
                        turnRestriction.active(state.getTimeSeconds())) {
                    return false;
                }
            } else {
                if (e.isEquivalentTo(turnRestriction.to) && turnRestriction.modes.contains(mode) &&
                        turnRestriction.active(state.getTimeSeconds())) {
                    return false;
                }
            }
        }
        return true;
    }

	/**
	* Gets non-localized I18NString (Used when splitting edges)
	* @return non-localized Name
	*/
	public I18NString getName() {
		return this.name;
	}

	public void setName(I18NString name) {
		this.name = name;
	}

	public LineString getGeometry() {
		return CompactLineString.uncompactLineString(fromv.getLon(), fromv.getLat(), tov.getLon(), tov.getLat(), compactGeometry, isBack());
	}

	private void setGeometry(LineString geometry) {
		this.compactGeometry = CompactLineString.compactLineString(fromv.getLon(), fromv.getLat(), tov.getLon(), tov.getLat(), isBack() ? (LineString)geometry.reverse() : geometry, isBack());
	}

	public void shareData(StreetEdge reversedEdge) {
	    if (Arrays.equals(compactGeometry, reversedEdge.compactGeometry)) {
	        compactGeometry = reversedEdge.compactGeometry;
	    } else {
	        LOG.warn("Can't share geometry between {} and {}", this, reversedEdge);
	    }
	}

	public boolean isWheelchairAccessible() {
		return BitSetUtils.get(flags, WHEELCHAIR_ACCESSIBLE_FLAG_INDEX);
	}

	public void setWheelchairAccessible(boolean wheelchairAccessible) {
        flags = BitSetUtils.set(flags, WHEELCHAIR_ACCESSIBLE_FLAG_INDEX, wheelchairAccessible);
	}

	public StreetTraversalPermission getPermission() {
		return permission;
	}

	public void setPermission(StreetTraversalPermission permission) {
		this.permission = permission;
	}

	public int getStreetClass() {
		return streetClass;
	}

	public void setStreetClass(int streetClass) {
		this.streetClass = streetClass;
	}

	/**
	 * Marks that this edge is the reverse of the one defined in the source
	 * data. Does NOT mean fromv/tov are reversed.
	 */
	public boolean isBack() {
	    return BitSetUtils.get(flags, BACK_FLAG_INDEX);
	}

	public void setBack(boolean back) {
            flags = BitSetUtils.set(flags, BACK_FLAG_INDEX, back);
	}

	public boolean isRoundabout() {
            return BitSetUtils.get(flags, ROUNDABOUT_FLAG_INDEX);
	}

	public void setRoundabout(boolean roundabout) {
	    flags = BitSetUtils.set(flags, ROUNDABOUT_FLAG_INDEX, roundabout);
	}

	public boolean hasBogusName() {
	    return BitSetUtils.get(flags, HASBOGUSNAME_FLAG_INDEX);
	}

	public void setHasBogusName(boolean hasBogusName) {
	    flags = BitSetUtils.set(flags, HASBOGUSNAME_FLAG_INDEX, hasBogusName);
	}

        public boolean isWalkNoThruTraffic() {
            return BitSetUtils.get(flags, WALK_NOTHRUTRAFFIC);
        }

        public void setWalkNoThruTraffic(boolean noThruTraffic) {
            flags = BitSetUtils.set(flags, WALK_NOTHRUTRAFFIC, noThruTraffic);
        }

        public boolean isMotorVehicleNoThruTraffic() {
            return BitSetUtils.get(flags, MOTOR_VEHICLE_NOTHRUTRAFFIC);
        }

        public void setMotorVehicleNoThruTraffic(boolean noThruTraffic) {
            flags = BitSetUtils.set(flags, MOTOR_VEHICLE_NOTHRUTRAFFIC, noThruTraffic);
        }

        public boolean isBicycleNoThruTraffic() {
            return BitSetUtils.get(flags, BICYCLE_NOTHRUTRAFFIC);
        }

        public void setBicycleNoThruTraffic(boolean noThruTraffic) {
            flags = BitSetUtils.set(flags, BICYCLE_NOTHRUTRAFFIC, noThruTraffic);
        }

	/**
	 * This street is a staircase
	 */
	public boolean isStairs() {
            return BitSetUtils.get(flags, STAIRS_FLAG_INDEX);
	}

	public void setStairs(boolean stairs) {
	    flags = BitSetUtils.set(flags, STAIRS_FLAG_INDEX, stairs);
	}

	public float getCarSpeed() {
		return carSpeed;
	}

	public void setCarSpeed(float carSpeed) {
		this.carSpeed = carSpeed;
	}

	public boolean isSlopeOverride() {
	    return BitSetUtils.get(flags, SLOPEOVERRIDE_FLAG_INDEX);
	}

	public void setSlopeOverride(boolean slopeOverride) {
	    flags = BitSetUtils.set(flags, SLOPEOVERRIDE_FLAG_INDEX, slopeOverride);
	}

    /**
     * Return the azimuth of the first segment in this edge in integer degrees clockwise from South.
     * TODO change everything to clockwise from North
     */
	public int getInAngle() {
		return (int) Math.round(this.inAngle * 180 / 128.0);
	}

    /** Return the azimuth of the last segment in this edge in integer degrees clockwise from South. */
	public int getOutAngle() {
		return (int) Math.round(this.outAngle * 180 / 128.0);
	}

    public void setCostExtension(StreetEdgeCostExtension costExtension) {
        this.costExtension = costExtension;
    }

    /** Split this street edge and return the resulting street edges. After splitting, the original
     * edge will be removed from the graph. */
    public P2<StreetEdge> splitDestructively(SplitterVertex v) {
        P2<LineString> geoms = GeometryUtils.splitGeometryAtPoint(getGeometry(), v.getCoordinate());

        StreetEdge e1 = new StreetEdge((StreetVertex) fromv, v, geoms.first, name, permission, this.isBack());
        StreetEdge e2 = new StreetEdge(v, (StreetVertex) tov, geoms.second, name, permission, this.isBack());

        // copy the wayId to the split edges, so we can trace them back to their parent if need be
        e1.wayId = this.wayId;
        e2.wayId = this.wayId;

        // we have this code implemented in both directions, because splits are fudged half a millimeter
        // when the length of this is odd. We want to make sure the lengths of the split streets end up
        // exactly the same as their backStreets so that if they are split again the error does not accumulate
        // and so that the order in which they are split does not matter.
        if (!isBack()) {
            // cast before the divide so that the sum is promoted
            double frac = (double) e1.length_mm / (e1.length_mm + e2.length_mm);
            e1.length_mm = (int) (length_mm * frac);
            e2.length_mm = length_mm - e1.length_mm;
        }
        else {
            // cast before the divide so that the sum is promoted
            double frac = (double) e2.length_mm / (e1.length_mm + e2.length_mm);
            e2.length_mm = (int) (length_mm * frac);
            e1.length_mm = length_mm - e2.length_mm;
        }

        // TODO: better handle this temporary fix to handle bad edge distance calculation
        if (e1.length_mm <= 0) {
            LOG.error("Edge 1 ({}) split at vertex at {},{} has length {} mm. Setting to 1 mm.", e1.wayId, v.getLat(), v.getLon(), e1.length_mm);
            e1.length_mm = 1;
        }
        if (e2.length_mm <= 0) {
            LOG.error("Edge 2 ({}) split at vertex at {},{}  has length {} mm. Setting to 1 mm.", e2.wayId, v.getLat(), v.getLon(), e2.length_mm);
            e2.length_mm = 1;
        }

        if (e1.length_mm < 0 || e2.length_mm < 0) {
            e1.tov.removeIncoming(e1);
            e1.fromv.removeOutgoing(e1);
            e2.tov.removeIncoming(e2);
            e2.fromv.removeOutgoing(e2);
            throw new IllegalStateException("Split street is longer than original street!");
        }

        for (StreetEdge e : new StreetEdge[] { e1, e2 }) {
            e.setBicycleSafetyFactor(getBicycleSafetyFactor());
            e.setHasBogusName(hasBogusName());
            e.setStairs(isStairs());
            e.setWheelchairAccessible(isWheelchairAccessible());
            e.setBack(isBack());
        }

        var splitEdges = new P2<>(e1, e2);
        copyRestrictionsToSplitEdges(this, splitEdges);
        return splitEdges;
    }


    /** Split this street edge and return the resulting street edges. The original edge is kept. */
    public P2<StreetEdge> splitNonDestructively(
        SplitterVertex v,
        DisposableEdgeCollection tempEdges,
        LinkingDirection direction
    ) {
        P2<LineString> geoms = GeometryUtils.splitGeometryAtPoint(getGeometry(), v.getCoordinate());

        StreetEdge e1 = null;
        StreetEdge e2 = null;

        if (direction == LinkingDirection.OUTGOING || direction == LinkingDirection.BOTH_WAYS) {
            e1 = new TemporaryPartialStreetEdge(this, (StreetVertex) fromv, v, geoms.first, name);
            e1.setMotorVehicleNoThruTraffic(this.isMotorVehicleNoThruTraffic());
            e1.setBicycleNoThruTraffic(this.isBicycleNoThruTraffic());
            e1.setWalkNoThruTraffic(this.isWalkNoThruTraffic());
            e1.setStreetClass(this.getStreetClass());
            e1.setStairs(this.isStairs());
            tempEdges.addEdge(e1);
        }
        if (direction == LinkingDirection.INCOMING || direction == LinkingDirection.BOTH_WAYS) {
            e2 = new TemporaryPartialStreetEdge(this, v, (StreetVertex) tov, geoms.second, name);
            e2.setMotorVehicleNoThruTraffic(this.isMotorVehicleNoThruTraffic());
            e2.setBicycleNoThruTraffic(this.isBicycleNoThruTraffic());
            e2.setWalkNoThruTraffic(this.isWalkNoThruTraffic());
            e2.setStreetClass(this.getStreetClass());
            e2.setStairs(this.isStairs());
            tempEdges.addEdge(e2);
        }

        var splitEdges = new P2<>(e1, e2);
        copyRestrictionsToSplitEdges(this, splitEdges);
        return splitEdges;
    }

    /**
     * Copy restrictions having former edge as from to appropriate split edge, as well as
     * restrictions on incoming edges.
     */
    private static void copyRestrictionsToSplitEdges(StreetEdge edge, P2<StreetEdge> splitEdges) {

        edge.getTurnRestrictions().forEach(restriction -> {
            // figure which one is the "from" edge
            StreetEdge fromEdge = shouldUseFirstSplitEdge(edge, restriction) ? splitEdges.first : splitEdges.second;

            TurnRestriction splitTurnRestriction = new TurnRestriction(fromEdge, restriction.to,
                    restriction.type, restriction.modes, restriction.time
            );
            LOG.debug(
                    "Recreate new restriction {} with split edge as from edge {}", splitTurnRestriction,
                    fromEdge
            );
            fromEdge.addTurnRestriction(splitTurnRestriction);
            // Not absolutely necessary, as old edge will not be accessible, but for good housekeeping
            edge.removeTurnRestriction(restriction);
        });

        applyToAdjacentEdges(edge, splitEdges.second, edge.getToVertex().getOutgoing());
        applyToAdjacentEdges(edge, splitEdges.first, edge.getFromVertex().getIncoming());
    }

    private static boolean shouldUseFirstSplitEdge(StreetEdge edge, TurnRestriction restriction) {
        return restriction.to.getToVertex() == edge.getToVertex();
    }

    private static void applyToAdjacentEdges(
            StreetEdge formerEdge,
            StreetEdge newToEdge,
            Collection<Edge> adjacentEdges
    ) {
        adjacentEdges.stream()
                .filter(StreetEdge.class::isInstance)
                .map(StreetEdge.class::cast)
                .flatMap(originatingEdge -> originatingEdge.getTurnRestrictions().stream())
                .filter(restriction -> restriction.to == formerEdge)
                .forEach(restriction -> applyRestrictionsToNewEdge(newToEdge, restriction));
    }

    private static void applyRestrictionsToNewEdge(
            StreetEdge newEdge,
            TurnRestriction restriction
    ) {
        TurnRestriction splitTurnRestriction = new TurnRestriction(restriction.from,
                newEdge, restriction.type, restriction.modes, restriction.time
        );
        LOG.debug(
                "Recreate new restriction {} with split edge as to edge {}", splitTurnRestriction,
                newEdge
        );
        restriction.from.addTurnRestriction(splitTurnRestriction);
        // Former turn restriction needs to be removed. Especially no only_turn
        // restriction to a non-existent edge must not survive
        restriction.from.removeTurnRestriction(restriction);
    }

    /**
     * Add a {@link TurnRestriction} to this edge.
     *
     * This method is thread-safe as modifying the underlying set is synchronized.
     */
    public void addTurnRestriction(TurnRestriction turnRestriction) {
        if (turnRestriction == null) { return; }
        synchronized (this) {
            // in order to guarantee fast access without extra allocations
            // we make the turn restrictions unmodifiable after a copy-on-write modification
            var temp = new HashSet<>(turnRestrictions);
            temp.add(turnRestriction);
            turnRestrictions = List.copyOf(temp);
        }
    }

    /**
     * Remove a {@link TurnRestriction} from this edge.
     *
     * This method is thread-safe as modifying the underlying set is synchronized.
     */
    public void removeTurnRestriction(TurnRestriction turnRestriction) {
        if (turnRestriction == null) { return; }
        synchronized (this) {
            if (turnRestrictions.contains(turnRestriction)) {
                if (turnRestrictions.size() == 1) {
                    turnRestrictions = List.of();
                }
                else {
                    // in order to guarantee fast access without extra allocations
                    // we make the turn restrictions unmodifiable after a copy-on-write modification
                    var withRemoved = new HashSet<>(turnRestrictions);
                    withRemoved.remove(turnRestriction);
                    turnRestrictions = List.copyOf(withRemoved);
                }
            }
        }
    }

    /**
     * Get the immutable {@link Set} of {@link TurnRestriction} that belongs to this {@link StreetEdge}.
     *
     * This method is thread-safe, even if {@link StreetEdge#addTurnRestriction}
     * or {@link StreetEdge#removeTurnRestriction} is called concurrently.
     *
     */
    @Nonnull
    public Collection<TurnRestriction> getTurnRestrictions() {
        // this can be safely returned as it's unmodifiable
        return turnRestrictions;
    }

    /**
     * Get the starting OSM node ID of this edge. Note that this information is preserved when an
     * edge is split, so both edges will have the same starting and ending nodes.
     */
    public long getStartOsmNodeId () {
        if (fromv instanceof OsmVertex) {
            return ((OsmVertex) fromv).nodeId;
        }
        // get information from the splitter vertex so this edge gets the same traffic information it got before
        // it was split.
        else if (fromv instanceof SplitterVertex) {
            return ((SplitterVertex) fromv).previousNodeId;
        }
        else {
            return -1;
        }
    }

    /**
     * Get the ending OSM node ID of this edge. Note that this information is preserved when an
     * edge is split, so both edges will have the same starting and ending nodes.
     */
    public long getEndOsmNodeId () {
        if (tov instanceof OsmVertex) {
            return ((OsmVertex) tov).nodeId;
        }
            // get information from the splitter vertex so this edge gets the same traffic information it got before
            // it was split.
        else if (tov instanceof SplitterVertex) {
            return ((SplitterVertex) tov).nextNodeId;
        }
        else {
            return -1;
        }
    }
}
