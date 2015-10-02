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

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.TurnRestrictionType;
import org.opentripplanner.common.geometry.*;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.*;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.vertextype.BarrierVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.traffic.StreetSpeedSnapshot;
import org.opentripplanner.util.BitSetUtils;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * This represents a street segment.
 * 
 * @author novalis
 * 
 */
public class StreetEdge extends Edge implements Cloneable {

    private static Logger LOG = LoggerFactory.getLogger(StreetEdge.class);

    private static final long serialVersionUID = 1L;

    /* TODO combine these with OSM highway= flags? */
    public static final int CLASS_STREET = 3;
    public static final int CLASS_CROSSING = 4;
    public static final int CLASS_OTHERPATH = 5;
    public static final int CLASS_OTHER_PLATFORM = 8;
    public static final int CLASS_TRAIN_PLATFORM = 16;
    public static final int ANY_PLATFORM_MASK = 24;
    public static final int CROSSING_CLASS_MASK = 7; // ignore platform
    public static final int CLASS_LINK = 32; // on/offramps; OSM calls them "links"

    private static final double GREENWAY_SAFETY_FACTOR = 0.1;

    // TODO(flamholz): do something smarter with the car speed here.
    public static final float DEFAULT_CAR_SPEED = 11.2f;

    /** If you have more than 8 flags, increase flags to short or int */
    private static final int BACK_FLAG_INDEX = 0;
    private static final int ROUNDABOUT_FLAG_INDEX = 1;
    private static final int HASBOGUSNAME_FLAG_INDEX = 2;
    private static final int NOTHRUTRAFFIC_FLAG_INDEX = 3;
    private static final int STAIRS_FLAG_INDEX = 4;
    private static final int SLOPEOVERRIDE_FLAG_INDEX = 5;
    private static final int WHEELCHAIR_ACCESSIBLE_FLAG_INDEX = 6;

    /** back, roundabout, stairs, ... */
    private byte flags;

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

    private int[] compactGeometry;
    
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

    public StreetEdge(StreetVertex v1, StreetVertex v2, LineString geometry,
                      I18NString name, double length,
                      StreetTraversalPermission permission, boolean back) {
        super(v1, v2);
        this.setBack(back);
        this.setGeometry(geometry);
        this.length_mm = (int) (length * 1000); // CONVERT FROM FLOAT METERS TO FIXED MILLIMETERS
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
        if (options.wheelchairAccessible) {
            if (!isWheelchairAccessible()) {
                return false;
            }
            if (getMaxSlope() > options.maxSlope) {
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
    public double getDistance() {
        return length_mm / 1000.0; // CONVERT FROM FIXED MILLIMETERS TO FLOAT METERS
    }

    @Override
    public State traverse(State s0) {
        final RoutingRequest options = s0.getOptions();
        final TraverseMode currMode = s0.getNonTransitMode();
        StateEditor editor = doTraverse(s0, options, s0.getNonTransitMode());
        State state = (editor == null) ? null : editor.makeState();
        /* Kiss and ride support. Mode transitions occur without the explicit loop edges used in park-and-ride. */
        if (options.kissAndRide) {
            if (options.arriveBy) {
                // Branch search to "unparked" CAR mode ASAP after transit has been used.
                // Final WALK check prevents infinite recursion.
                if (s0.isCarParked() && s0.isEverBoarded() && currMode == TraverseMode.WALK) {
                    editor = doTraverse(s0, options, TraverseMode.CAR);
                    if (editor != null) {
                        editor.setCarParked(false); // Also has the effect of switching to CAR
                        State forkState = editor.makeState();
                        if (forkState != null) {
                            forkState.addToExistingResultChain(state);
                            return forkState; // return both parked and unparked states
                        }
                    }
                }
            } else { /* departAfter */
                // Irrevocable transition from driving to walking. "Parking" means being dropped off in this case.
                // Final CAR check needed to prevent infinite recursion.
                if ( ! s0.isCarParked() && ! getPermission().allows(TraverseMode.CAR) && currMode == TraverseMode.CAR) {
                    editor = doTraverse(s0, options, TraverseMode.WALK);
                    if (editor != null) {
                        editor.setCarParked(true); // has the effect of switching to WALK and preventing further car use
                        return editor.makeState(); // return only the "parked" walking state
                    }

                }
            }
        }
        return state;
    }

    /** return a StateEditor rather than a State so that we can make parking/mode switch modifications for kiss-and-ride. */
    private StateEditor doTraverse(State s0, RoutingRequest options, TraverseMode traverseMode) {
        boolean walkingBike = options.walkingBike;
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

        // Ensure we are actually walking, when walking a bike
        backWalkingBike &= TraverseMode.WALK.equals(backMode);
        walkingBike &= TraverseMode.WALK.equals(traverseMode);

        /* Check whether this street allows the current mode. If not and we are biking, attempt to walk the bike. */
        if (!canTraverse(options, traverseMode)) {
            if (traverseMode == TraverseMode.BICYCLE) {
                return doTraverse(s0, options.bikeWalkingOptions, TraverseMode.WALK);
            }
            return null;
        }

        // Automobiles have variable speeds depending on the edge type
        double speed = calculateSpeed(options, traverseMode, s0.getTimeInMillis());
        
        double time = getDistance() / speed;
        double weight;
        // TODO(flamholz): factor out this bike, wheelchair and walking specific logic to somewhere central.
        if (options.wheelchairAccessible) {
            weight = getSlopeSpeedEffectiveLength() / speed;
        } else if (traverseMode.equals(TraverseMode.BICYCLE)) {
            time = getSlopeSpeedEffectiveLength() / speed;
            switch (options.optimize) {
            case SAFE:
                weight = bicycleSafetyFactor * getDistance() / speed;
                break;
            case GREENWAYS:
                weight = bicycleSafetyFactor * getDistance() / speed;
                if (bicycleSafetyFactor <= GREENWAY_SAFETY_FACTOR) {
                    // greenways are treated as even safer than they really are
                    weight *= 0.66;
                }
                break;
            case FLAT:
                /* see notes in StreetVertex on speed overhead */
                weight = getDistance() / speed + getSlopeWorkCostEffectiveLength();
                break;
            case QUICK:
                weight = getSlopeSpeedEffectiveLength() / speed;
                break;
            case TRIANGLE:
                double quick = getSlopeSpeedEffectiveLength();
                double safety = bicycleSafetyFactor * getDistance();
                // TODO This computation is not coherent with the one for FLAT
                double slope = getSlopeWorkCostEffectiveLength();
                weight = quick * options.triangleTimeFactor + slope
                        * options.triangleSlopeFactor + safety
                        * options.triangleSafetyFactor;
                weight /= speed;
                break;
            default:
                weight = getDistance() / speed;
            }
        } else {
            if (walkingBike) {
                // take slopes into account when walking bikes
                time = getSlopeSpeedEffectiveLength() / speed;
            }
            weight = time;
            if (traverseMode.equals(TraverseMode.WALK)) {
                // take slopes into account when walking
                // FIXME: this causes steep stairs to be avoided. see #1297.
                double costs = ElevationUtils.getWalkCostsForSlope(getDistance(), getMaxSlope());
                // as the cost walkspeed is assumed to be for 4.8km/h (= 1.333 m/sec) we need to adjust
                // for the walkspeed set by the user
                double elevationUtilsSpeed = 4.0 / 3.0;
                weight = costs * (elevationUtilsSpeed / speed);
                time = weight; //treat cost as time, as in the current model it actually is the same (this can be checked for maxSlope == 0)
                /*
                // debug code
                if(weight > 100){
                    double timeflat = length / speed;
                    System.out.format("line length: %.1f m, slope: %.3f ---> slope costs: %.1f , weight: %.1f , time (flat):  %.1f %n", length, elevationProfile.getMaxSlope(), costs, weight, timeflat);
                }
                */
            }
        }

        if (isStairs()) {
            weight *= options.stairsReluctance;
        } else {
            // TODO: this is being applied even when biking or driving.
            weight *= options.walkReluctance;
        }

        StateEditor s1 = s0.edit(this);
        s1.setBackMode(traverseMode);
        s1.setBackWalkingBike(walkingBike);

        /* Handle no through traffic areas. */
        if (this.isNoThruTraffic()) {
            // Record transition into no-through-traffic area.
            if (backEdge instanceof StreetEdge && !((StreetEdge)backEdge).isNoThruTraffic()) {
                s1.setEnteredNoThroughTrafficArea();
            }
            // If we transitioned into a no-through-traffic area at some point, check if we are exiting it.
            if (s1.hasEnteredNoThroughTrafficArea()) {
                // Only Edges are marked as no-thru, but really we need to avoid creating dominant, pruned states
                // on thru _Vertices_. This could certainly be improved somehow.
                for (StreetEdge se : Iterables.filter(s1.getVertex().getOutgoing(), StreetEdge.class)) {
                    if (!se.isNoThruTraffic()) {
                        // This vertex has at least one through-traffic edge. We can't dominate it with a no-thru state.
                        return null;
                    }
                }
            }
        }

        /* Compute turn cost. */
        StreetEdge backPSE;
        if (backEdge != null && backEdge instanceof StreetEdge) {
            backPSE = (StreetEdge) backEdge;
            RoutingRequest backOptions = backWalkingBike ?
                    s0.getOptions().bikeWalkingOptions : s0.getOptions();
            double backSpeed = backPSE.calculateSpeed(backOptions, backMode, s0.getTimeInMillis());
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

            long turnTime = (long) Math.ceil(realTurnCost);
            time += turnTime;
            weight += options.turnReluctance * realTurnCost;
        }
        

        if (walkingBike || TraverseMode.BICYCLE.equals(traverseMode)) {
            if (!(backWalkingBike || TraverseMode.BICYCLE.equals(backMode))) {
                s1.incrementTimeInSeconds(options.bikeSwitchTime);
                s1.incrementWeight(options.bikeSwitchCost);
            }
        }

        if (!traverseMode.isDriving()) {
            s1.incrementWalkDistance(getDistance());
        }

        /* On the pre-kiss/pre-park leg, limit both walking and driving, either soft or hard. */
        int roundedTime = (int) Math.ceil(time);
        if (options.kissAndRide || options.parkAndRide) {
            if (options.arriveBy) {
                if (!s0.isCarParked()) s1.incrementPreTransitTime(roundedTime);
            } else {
                if (!s0.isEverBoarded()) s1.incrementPreTransitTime(roundedTime);
            }
            if (s1.isMaxPreTransitTimeExceeded(options)) {
                if (options.softPreTransitLimiting) {
                    weight += calculateOverageWeight(s0.getPreTransitTime(), s1.getPreTransitTime(),
                            options.maxPreTransitTime, options.preTransitPenalty,
                                    options.preTransitOverageRate);
                } else return null;
            }
        }
        
        /* Apply a strategy for avoiding walking too far, either soft (weight increases) or hard limiting (pruning). */
        if (s1.weHaveWalkedTooFar(options)) {

            // if we're using a soft walk-limit
            if( options.softWalkLimiting ){
                // just slap a penalty for the overage onto s1
                weight += calculateOverageWeight(s0.getWalkDistance(), s1.getWalkDistance(),
                        options.getMaxWalkDistance(), options.softWalkPenalty,
                                options.softWalkOverageRate);
            } else {
                // else, it's a hard limit; bail
                LOG.debug("Too much walking. Bailing.");
                return null;
            }
        }

        s1.incrementTimeInSeconds(roundedTime);
        
        s1.incrementWeight(weight);

        return s1;
    }

    private double calculateOverageWeight(double firstValue, double secondValue, double maxValue,
            double softPenalty, double overageRate) {
        // apply penalty if we stepped over the limit on this traversal
        boolean applyPenalty = false;
        double overageValue;

        if(firstValue <= maxValue && secondValue > maxValue){
            applyPenalty = true;
            overageValue = secondValue - maxValue;
        } else {
            overageValue = secondValue - firstValue;
        }

        // apply overage and add penalty if necessary
        return (overageRate * overageValue) + (applyPenalty ? softPenalty : 0.0);
    }

    /**
     * Calculate the average automobile traversal speed of this segment, given
     * the RoutingRequest, and return it in meters per second.
     */
    private double calculateCarSpeed(RoutingRequest options) {
        return getCarSpeed();
    }
    
    /**
     * Calculate the speed appropriately given the RoutingRequest and traverseMode and the current wall clock time.
     * Note: this is not strictly symmetrical, because in a forward search we get the speed based on the
     * time we enter this edge, whereas in a reverse search we get the speed based on the time we exit
     * the edge.
     */
    public double calculateSpeed(RoutingRequest options, TraverseMode traverseMode, long timeMillis) {
        if (traverseMode == null) {
            return Double.NaN;
        } else if (traverseMode.isDriving()) {
            // NOTE: Automobiles have variable speeds depending on the edge type
            if (options.useTraffic) {
                // the expected speed based on traffic
                StreetSpeedSnapshot source = options.getRoutingContext().streetSpeedSnapshot;

                if (source != null) {
                    double congestedSpeed = source.getSpeed(this, traverseMode, timeMillis);

                    if (!Double.isNaN(congestedSpeed))
                        return congestedSpeed;
                }
            }

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
        return this.getDistance() / options.getStreetSpeedUpperBound();
    }

    public double getSlopeSpeedEffectiveLength() {
        return getDistance();
    }

    public double getSlopeWorkCostEffectiveLength() {
        return getDistance();
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
        return "StreetEdge(" + getId() + ", " + name + ", " + fromv + " -> " + tov
                + " length=" + this.getDistance() + " carSpeed=" + this.getCarSpeed()
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
        for (TurnRestriction turnRestriction : getTurnRestrictions(state.getOptions().rctx.graph)) {
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

	@Override
	public String getName() {
		return this.name.toString();
	}

	/**
	* Gets non-localized I18NString (Used when splitting edges)
	* @return non-localized Name
	*/
	public I18NString getRawName() {
		return this.name;
	}

	public String getName(Locale locale) {
		return this.name.toString(locale);
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

	public boolean isNoThruTraffic() {
            return BitSetUtils.get(flags, NOTHRUTRAFFIC_FLAG_INDEX);
	}

	public void setNoThruTraffic(boolean noThruTraffic) {
	    flags = BitSetUtils.set(flags, NOTHRUTRAFFIC_FLAG_INDEX, noThruTraffic);
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
		return this.inAngle * 180 / 128;
	}

    /** Return the azimuth of the last segment in this edge in integer degrees clockwise from South. */
	public int getOutAngle() {
		return this.outAngle * 180 / 128;
	}

    protected List<TurnRestriction> getTurnRestrictions(Graph graph) {
        return graph.getTurnRestrictions(this);
    }
    
    /** calculate the length of this street segement from its geometry */
    protected void calculateLengthFromGeometry () {
        double accumulatedMeters = 0;

        LineString geom = getGeometry();

        for (int i = 1; i < geom.getNumPoints(); i++) {
            accumulatedMeters += SphericalDistanceLibrary.distance(geom.getCoordinateN(i - 1), geom.getCoordinateN(i));
        }

        length_mm = (int) (accumulatedMeters * 1000);
    }

    /** Split this street edge and return the resulting street edges */
    public P2<StreetEdge> split (SplitterVertex v) {
        P2<LineString> geoms = GeometryUtils.splitGeometryAtPoint(getGeometry(), v.getCoordinate());
        StreetEdge e1 = new StreetEdge((StreetVertex) fromv, v, geoms.first, name, 0, permission, this.isBack());
        StreetEdge e2 = new StreetEdge(v, (StreetVertex) tov, geoms.second, name, 0, permission, this.isBack());

        // figure the lengths, ensuring that they sum to the length of this edge
        e1.calculateLengthFromGeometry();
        e2.calculateLengthFromGeometry();

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

        return new P2<StreetEdge>(e1, e2);
    }

    /**
     * Get the starting OSM node ID of this edge. Note that this information is preserved when an
     * edge is split, so both edges will have the same starting and ending nodes.
     */
    public long getStartOsmNodeId () {
        if (fromv instanceof OsmVertex)
            return ((OsmVertex) fromv).nodeId;
        // get information from the splitter vertex so this edge gets the same traffic information it got before
        // it was split.
        else if (fromv instanceof SplitterVertex)
            return ((SplitterVertex) fromv).previousNodeId;
        else
            return -1;
    }

    /**
     * Get the ending OSM node ID of this edge. Note that this information is preserved when an
     * edge is split, so both edges will have the same starting and ending nodes.
     */
    public long getEndOsmNodeId () {
        if (tov instanceof OsmVertex)
            return ((OsmVertex) tov).nodeId;
            // get information from the splitter vertex so this edge gets the same traffic information it got before
            // it was split.
        else if (tov instanceof SplitterVertex)
            return ((SplitterVertex) tov).nextNodeId;
        else
            return -1;
    }
}
