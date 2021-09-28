package org.opentripplanner.routing.edgetype;

import com.google.common.collect.Iterables;
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
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.vertextype.BarrierVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.OsmVertex;
import org.opentripplanner.routing.vertextype.SemiPermanentSplitterVertex;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporarySplitterVertex;
import org.opentripplanner.util.BitSetUtils;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * This represents a street segment.
 * 
 * @author novalis
 * 
 */
public class StreetEdge extends Edge implements Cloneable {
    private static Logger LOG = LoggerFactory.getLogger(StreetEdge.class);

    private static final long serialVersionUID = 2L;

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

    /** The walk comfort score. Applied as multiplier to edge weight; 1.0 is baseline */
    protected float walkComfortScore;

    /**
     *  Map of OSM tags for this way. Only stored when 'includeOsmWays' builder param is true;
     *  enables on-the-fly recalculation of walk comfort scores for testing/calibration purposes.
     */
    private Map<String, String> osmTags;

    /**
     * A set of car networks where this edge is located inside their service regions.
     */
    private Set<String> carNetworks;

    /**
     * A set of vehicle networks where this edge is located inside their service regions.
     */
    private Set<String> vehicleNetworks;

    // whether or not this street is a good place to board or alight a TNC vehicle
    private boolean suitableForTNCStop = true;

    // whether or not this street is a good place to dropoff a floating car rental
    private boolean suitableForFloatingCarRentalDropoff = true;

    // whether or not this street is a good place to dropoff a floating vehicle rental
    private boolean suitableForFloatingVehicleRentalDropoff = true;

    public StreetEdge(StreetVertex v1, StreetVertex v2, LineString geometry,
                      I18NString name, double length,
                      StreetTraversalPermission permission, boolean back) {
        super(v1, v2);
        this.setBack(back);
        this.setGeometry(geometry);
        this.length_mm = (int) (length * 1000); // CONVERT FROM FLOAT METERS TO FIXED MILLIMETERS
        this.bicycleSafetyFactor = 1.0f;
        this.walkComfortScore = 1.0f;
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
        } else if (options.useTransportationNetworkCompany) {
            // Irrevocable transition from using hailed car to walking.
            // Final CAR check needed to prevent infinite recursion.
            if (
                s0.isUsingHailedCar() &&
                    !getPermission().allows(TraverseMode.CAR) &&
                    currMode == TraverseMode.CAR
            ) {
                if (!s0.isTNCStopAllowed()) {
                    return null;
                }
                editor = doTraverse(s0, options, TraverseMode.WALK);
                if (editor != null) {
                    editor.alightHailedCar(); // done with TNC use for now
                    return editor.makeState(); // return only the state with updated TNC usage
                }
            }
            // possible transition to hailing a car
            else if (
                !s0.isUsingHailedCar() &&
                    getPermission().allows(TraverseMode.CAR) &&
                    currMode != TraverseMode.CAR &&
                    getTNCStopSuitability()
            ) {
                // perform extra checks to prevent entering a tnc vehicle if a car has already been
                // hailed in the pre or post transit part of trip
                Vertex toVertex = options.rctx.toVertex;
                if (
                    // arriveBy searches
                    (
                        options.arriveBy && (
                            // forbid hailing car a 2nd time post transit
                            (!s0.isEverBoarded() && s0.stateData.hasHailedCarPostTransit()) ||
                            // forbid hailing car a 2nd time pre transit
                            (s0.isEverBoarded() && s0.stateData.hasHailedCarPreTransit())
                        )
                    )||
                    (
                        !options.arriveBy && (
                            // forbid hailing car a 2nd time pre transit
                            (!s0.isEverBoarded() && s0.stateData.hasHailedCarPreTransit()) ||
                            // forbid hailing car a 2nd time post transit
                            (s0.isEverBoarded() && s0.stateData.hasHailedCarPostTransit())
                        )
                    )
                ) {
                    return state;
                }

                StateEditor editorCar = doTraverse(s0, options, TraverseMode.CAR);
                StateEditor editorNonCar = doTraverse(s0, options, currMode);
                if (editorCar != null) {
                    editorCar.boardHailedCar(getDistance()); // start of TNC use
                    if (editorNonCar != null) {
                        // make the forkState be of the non-car mode so it's possible to build walk steps
                        State forkState = editorNonCar.makeState();
                        if (forkState != null) {
                            forkState.addToExistingResultChain(editorCar.makeState());
                            return forkState; // return both in-car and out-of-car states
                        } else {
                            // if the non-car state is non traversable or something, return just the car state
                            return editorCar.makeState();
                        }
                    } else {
                        // if the non-car state is non traversable or something, return just the car state
                        return editorCar.makeState();
                    }
                }
            }
        } else if (options.allowCarRental) {
            // Irrevocable transition from using rented car to walking.
            // Final CAR check needed to prevent infinite recursion.
            if (
                s0.isCarRenting() &&
                    !getPermission().allows(TraverseMode.CAR) &&
                    currMode == TraverseMode.CAR &&
                    // if in "arrive by" mode, the search is progressing backwards while in a car
                    // rental state, but encounters an edge that cannot be traversed using a car
                    // before encountering a car rental pickup station.  Therefore, this search
                    // cannot proceed.
                    !options.arriveBy &&
                    s0.isCarRentalDropoffAllowed(this, false)
            ) {
                editor = doTraverse(s0, options, TraverseMode.WALK);
                if (editor != null) {
                    editor.endCarRenting(); // done with car rental use for now
                    editor.incrementWeight(options.carRentalDropoffCost);
                    editor.incrementTimeInSeconds(options.carRentalDropoffTime);
                    return editor.makeState(); // return only the state with updated rental car usage
                }
            }
            // possible transition to dropping off a floating car when in "arrive by" mode
            else if (
                !s0.isCarRenting() &&
                    getPermission().allows(TraverseMode.CAR) &&
                    currMode != TraverseMode.CAR &&
                    options.arriveBy &&
                    s0.isCarRentalDropoffAllowed(this, false)
            ) {
                StateEditor editorCar = doTraverse(s0, options, TraverseMode.CAR);
                if (editorCar != null) {
                    // begin car rental usage.
                    editorCar.incrementWeight(options.carRentalPickupCost);
                    editorCar.incrementTimeInSeconds(options.carRentalPickupTime);
                    editorCar.beginCarRenting(getDistance(), carNetworks, true);
                    editorCar.setBackMode(TraverseMode.CAR);
                    if (state != null) {
                        // make the forkState be of the non-car mode so it's possible to build walk steps
                        state.addToExistingResultChain(editorCar.makeState());
                        return state; // return both in-car and out-of-car states
                    } else {
                        // if the non-car state is non traversable or something, return just the car state
                        return editorCar.makeState();
                    }
                }
            }
        } else if (options.allowVehicleRental) {
            // possible transitions out of renting a Micromobility vehicle during "depart at" searches
            if (
                !options.arriveBy &&
                    s0.isVehicleRenting() &&
                    currMode == TraverseMode.MICROMOBILITY
            ) {
                // A StreetEdge has been encountered that
                // 1. Does not allow Micromobility travel
                // 2. Allows a floating vehicle dropoff under the following cirucmstances:
                //    a. on the current edge
                //    b. on the edge of the previous state (NOTE: the previous StreetEdge is considered because it can
                //        be reasoned that the very last point of the previous StreetEdge constitutes a part of this
                //        current StreetEdge. Since walking would begin on this StreetEdge, the floating rental vehicle
                //        is assumed to be left at the very beginning of the StreetEdge.)
                //
                // In this case fork the state into 2 options:
                // 1. End the vehicle rental and begin walking
                // 2. Keep renting the vehicle, but transition to WALK mode
                if (
                    !getPermission().allows(TraverseMode.MICROMOBILITY) &&
                        (
                            s0.isVehicleRentalDropoffAllowed(this, false) ||
                                (
                                    s0.backEdge instanceof StreetEdge &&
                                        s0.isVehicleRentalDropoffAllowed(
                                            (StreetEdge) s0.backEdge,
                                            false
                                        )
                                )
                        )
                ) {
                    StateEditor editorEndedVehicleRental = doTraverse(s0, options, TraverseMode.WALK);
                    StateEditor editorKeepVehicleRental = doTraverse(s0, options, TraverseMode.WALK);
                    State keepVehicleRentalState = null;
                    if (editorKeepVehicleRental != null) {
                        editorKeepVehicleRental.setBackMode(TraverseMode.WALK);
                        keepVehicleRentalState = editorKeepVehicleRental.makeState();
                    }
                    if (editorEndedVehicleRental != null) {
                        editorEndedVehicleRental.endVehicleRenting(); // done with vehicle rental use for now
                        editorEndedVehicleRental.incrementWeight(options.vehicleRentalDropoffCost);
                        editorEndedVehicleRental.incrementTimeInSeconds(options.vehicleRentalDropoffTime);
                        State endedVehicleRentalState = editorEndedVehicleRental.makeState();
                        if (endedVehicleRentalState != null) {
                            endedVehicleRentalState.addToExistingResultChain(keepVehicleRentalState);
                            return endedVehicleRentalState;
                        }
                    }
                    return keepVehicleRentalState;
                }
                // A StreetEdge has been ecountered where:
                // 1. Micromobility vehicles are allowed to be ridden
                // 2. Micromobility vehicles are not allowed to be dropped off at
                // 3. The previous edge was a StreetEdge that did allow a floating vehicle dropoff
                //
                // In this case, return 2 states:
                // 1. Still renting and riding the vehicle
                // 2. Vehicle rental ended and walking on foot
                else if (
                    getPermission().allows(TraverseMode.MICROMOBILITY) &&
                        !getFloatingVehicleDropoffSuitability() &&
                        s0.backEdge instanceof StreetEdge &&
                        s0.isVehicleRentalDropoffAllowed((StreetEdge) s0.backEdge, false)
                ) {
                    StateEditor editorEndedVehicleRental = doTraverse(s0, options, TraverseMode.WALK);
                    if (editorEndedVehicleRental != null) {
                        editorEndedVehicleRental.endVehicleRenting(); // done with vehicle rental use for now
                        editorEndedVehicleRental.incrementWeight(options.vehicleRentalDropoffCost);
                        editorEndedVehicleRental.incrementTimeInSeconds(options.vehicleRentalDropoffTime);
                        State endedVehicleRentalState = editorEndedVehicleRental.makeState();
                        if (endedVehicleRentalState != null) {
                            endedVehicleRentalState.addToExistingResultChain(state);
                            return endedVehicleRentalState;
                        }
                    }
                    return state;
                }
            }
            // possible backward transition of completing a dropping off of a floating vehicle when in "arrive by" mode
            // NOTE: the previous StreetEdge is considered because it can be reasoned that the very first point of the
            // previous StreetEdge constitutes a part of this current StreetEdge. Since a vehicle rental could end on
            // the previous StreetEdge, the floating rental vehicle is assumed to be left at the very end of the
            // StreetEdge.
            else if (
                !s0.isVehicleRenting() &&
                    (getPermission().allows(TraverseMode.MICROMOBILITY) || getPermission().allows(TraverseMode.WALK)) &&
                    currMode != TraverseMode.MICROMOBILITY &&
                    options.arriveBy &&
                    (s0.isVehicleRentalDropoffAllowed(this, false) ||
                        (
                            s0.backEdge instanceof StreetEdge &&
                                s0.isVehicleRentalDropoffAllowed(
                                    (StreetEdge) s0.backEdge,
                                    false
                                )
                        )
                    )
            ) {
                StateEditor editorWithVehicleRental = doTraverse(s0, options, TraverseMode.MICROMOBILITY);
                if (editorWithVehicleRental != null) {
                    // begin vehicle rental usage.
                    editorWithVehicleRental.incrementWeight(options.vehicleRentalPickupCost);
                    editorWithVehicleRental.incrementTimeInSeconds(options.vehicleRentalPickupTime);
                    editorWithVehicleRental.beginVehicleRenting(getDistance(), vehicleNetworks, true);
                    // in arriveBy mode transitions on a street edge, we must immediately set the backmode to
                    // Micromobility to make sure proper state is maintained for correct state slicing and reverse
                    // optimization. In rentAVehicleOn/OffEdges, the mode should not change until the following state to
                    // ensure that the state is sliced at the rentAVehicleOn/OffEdge. However, here, the slice must
                    // occur here. NOTE: if the edge is only traversable by walking, then the backMode should be set to
                    // walk
                    editorWithVehicleRental.setBackMode(
                        canTraverse(options, TraverseMode.MICROMOBILITY)
                            ? TraverseMode.MICROMOBILITY
                            : TraverseMode.WALK
                    );
                    State editorWithVehicleRentalState = editorWithVehicleRental.makeState();
                    if (state != null) {
                        // make the forkState be of the non-vehicle-rental mode so it's possible to build walk steps
                        if (editorWithVehicleRentalState != null) {
                            state.addToExistingResultChain(editorWithVehicleRentalState);
                        }
                        return state;
                    } else {
                        // if the no-rented-vehicle state is non traversable or something, return just the
                        // rented-vehicle state
                        return editorWithVehicleRentalState;
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
            if (traverseMode == TraverseMode.BICYCLE || traverseMode == TraverseMode.MICROMOBILITY) {
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
                double distance = getSlopeWalkSpeedEffectiveLength();
                weight = distance / speed;
                weight = weight * walkComfortScore;
                time = weight; //treat cost as time, as in the current model it actually is the same (this can be checked for maxSlope == 0)
                /*
                // debug code
                if(weight > 100){
                    double timeflat = length_mm / speed;


                    System.out.format("line length: %.1f m, slope: %.3f ---> distance: %.1f , weight: %.1f , time (flat):  %.1f %n", getDistance(), getMaxSlope(), distance, weight, timeflat);
                }
                */
            }
        }

        if (options.wheelchairAccessible && !isWheelchairAccessible()) {
            // Apply a time penalty, in addition to the cost penalty, so that accessible transfers
            // work. When we compute transfers we only look at the time and hence increasing just
            // the cost would not work:
            // https://github.com/ibi-group/OpenTripPlanner/blob/f2b375364985b8dd83f791950d955e3ec5c9cb34/src/main/java/org/opentripplanner/routing/algorithm/EarliestArrivalSearch.java#L76
            weight *= options.noWheelchairAccessOnStreetReluctance;
            time *= options.noWheelchairAccessOnStreetReluctance;
        }

        if (isStairs() && options.wheelchairAccessible) {
            weight *= options.wheelchairStairsReluctance;
            // Apply a time penalty, in addition to the cost penalty, so that accessible transfers
            // work. When we compute transfers we only look at the time and hence increasing just
            // the cost would not work:
            // https://github.com/ibi-group/OpenTripPlanner/blob/f2b375364985b8dd83f791950d955e3ec5c9cb34/src/main/java/org/opentripplanner/routing/algorithm/EarliestArrivalSearch.java#L76
            time *= options.wheelchairStairsReluctance;
        } else if (isStairs()) {
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

        int roundedTime = (int) Math.ceil(time);

        /* Compute turn cost. */
        StreetEdge backPSE;
        if (backEdge != null && backEdge instanceof StreetEdge) {
            backPSE = (StreetEdge) backEdge;
            RoutingRequest backOptions = backWalkingBike ?
                    s0.getOptions().bikeWalkingOptions : s0.getOptions();
            double backSpeed = backPSE.calculateSpeed(backOptions, backMode, s0.getTimeInMillis());
            final double realTurnCost;  // Units are seconds.
            boolean canRetryBannedTurnWithWalkTransition = traverseMode == TraverseMode.BICYCLE ||
                traverseMode == TraverseMode.MICROMOBILITY;

            // Apply turn restrictions
            if (options.arriveBy && !canTurnOnto(backPSE, s0, backMode)) {
                // A turn restriction exists that forbids this turn with the current traverseMode.
                // If using a bike, or micromobility, try again while walking
                if (canRetryBannedTurnWithWalkTransition) {
                    return doTraverse(s0, options.bikeWalkingOptions, TraverseMode.WALK);
                }
                // After trying again with switching to walking, the backMode will not have changed. Therefore, an
                // assumption is made that a transition from non-walking to walking occurs at the very end of the last
                // state. In this case it might be possible to make said turn if an immediate transition to walking is
                // assumed at the very end of the previous state.
                else if (
                    traverseMode == TraverseMode.WALK &&
                        backMode != TraverseMode.WALK &&
                        canTurnOnto(backPSE, s0, TraverseMode.WALK)
                ) {
                    // the turn is now possible with the assumption that a transition to walking occurred at the very
                    // end of the last StreetEdge of the back state.
                    // Do nothing here in order to continue the traversal and avoid marking the edge as non-traversable
                    // by returning null.
                } else {
                    return null;
                }
            } else if (!options.arriveBy && !backPSE.canTurnOnto(this, s0, traverseMode)) {
                // A turn restriction exists that forbids this turn with the current traverseMode.
                // If using a bike, or micromobility, try again while walking
                if (canRetryBannedTurnWithWalkTransition) {
                    return doTraverse(s0, options.bikeWalkingOptions, TraverseMode.WALK);
                }
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
        

        // add a cost for switching modes. This assumes that it's not possible to make Bicycle <> Micromobility switches
        if (walkingBike || TraverseMode.BICYCLE.equals(traverseMode) || TraverseMode.MICROMOBILITY.equals(traverseMode)) {
            if (!(backWalkingBike || TraverseMode.BICYCLE.equals(backMode) || TraverseMode.MICROMOBILITY.equals(backMode))) {
                s1.incrementTimeInSeconds(options.bikeSwitchTime);
                s1.incrementWeight(options.bikeSwitchCost);
            }
        }

        if (!traverseMode.isDriving()) {
            s1.incrementWalkDistance(getDistance());
            if (s0.isVehicleRenting()) {
                s1.incrementVehicleRentalDistance(getDistance());
            }
        } else {
            // check if driveTimeReluctance is defined (ie it is greater than 0)
            if (options.driveTimeReluctance > 0) {
                s1.incrementWeight(time * options.driveTimeReluctance);
            }
            if (options.driveDistanceReluctance > 0) {
                s1.incrementWeight(getDistance() * options.driveDistanceReluctance);
            }
            if (s0.isUsingHailedCar()) {
                s1.incrementTransportationNetworkCompanyDistance(getDistance());
            }
            if (s0.isCarRenting()) {
                s1.incrementCarRentalDistance(getDistance());
            }
        }

        // On itineraries with car mode enabled, limit both walking and driving before transit,
        // either soft or hard.
        // We can safely assume no limit on driving after transit as most TNC companies will drive
        // outside of the pickup boundaries.
        if (
            options.kissAndRide ||
                options.parkAndRide ||
                options.useTransportationNetworkCompany ||
                options.allowCarRental
        ) {
            if (options.arriveBy) {
                // use different determining factor depending on what type of search this is.
                if (
                    // if kiss/park and ride, check if car has not yet been parked
                    ((options.kissAndRide || options.parkAndRide) && !s0.isCarParked()) ||
                        // if car rentals are enabled, check if a car has been rented before transit
                        (options.allowCarRental && !s0.stateData.hasRentedCarPreTransit()) ||
                        // if car hailing is enabled, check if a car has been hailed before transit
                        (options.useTransportationNetworkCompany && !s0.stateData.hasHailedCarPreTransit())
                ) {
                    s1.incrementPreTransitTime(roundedTime);
                }
            } else {
                // there is no differentiation in depart at queries
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
            return calculateCarSpeed(options);
        } else if (traverseMode == TraverseMode.MICROMOBILITY) {
            return Math.min(
                calculateMicromobilitySpeed(
                    options.watts,
                    options.weight,
                    Math.atan(0), // 0 slope beta
                    getRollingResistanceCoefficient(),
                    ElevationUtils.ZERO_ELEVATION_DRAG_RESISTIVE_FORCE_COMPONENT,
                    options.minimumMicromobilitySpeed,
                    options.maximumMicromobilitySpeed
                ),
                // Micromobility vehicles must also obey the car speed limit
                carSpeed
            );
        }
        return options.getSpeed(traverseMode);
    }

    // TODO: use some kind of lookup of roadway type to get this number (ie if gravel increase value)
    public double getRollingResistanceCoefficient() {
        return 0.005;
    }

    @Override
    public double weightLowerBound(RoutingRequest options) {
        return timeLowerBound(options) * options.walkReluctance;
    }

    @Override
    public double timeLowerBound(RoutingRequest options) {
        return this.getDistance() / options.getStreetSpeedUpperBound();
    }

    /**
     * Calculate the approximate speed for a vehicle given slope data, available sustained power output, weight, rolling
     * resistance, aerodynamic drag and bounds on min/max speeds.
     *
     * This calculation is made using a formula derived from the equations relating to determing total resistive force
     * that is needed to be overcome to maintain a certain velocity. The website at
     * http://www.kreuzotter.de/english/espeed.htm goes into great detail regarding this and presents the following
     * equation which also appears on other websites such as https://www.gribble.org/cycling/power_v_speed.html.
     *
     * P = Cm * V * (Cd * A * ρ/2 * (V + W) ^ 2 + Frg + V * Crvn)
     *
     * where:
     * P = power in watts
     * Cm = Coefficient for power transmission losses and losses due to tire slippage
     * V = velocity in m/s
     * Cd = Coefficient of aerodynamic drag
     * A = frontal area in m^2
     * ρ = air density in kg/m^3
     * W = wind speed in m/s (if positive this is a headwind, if negative this is a tailwind)
     * Frg = Rolling friction (normalized on inclined plane) plus slope pulling force on inclined plane
     * Crvn = The coefficient for the dynamic rolling resistance, normalized to road inclination.
     *
     * This equation is then rearranged in an attempt to solve for V on their website as the following:
     *
     * V^3 + V^2 * 2 * (W + Crvn / (Cd * A * ρ)) + V * (W^2 + (2 * Frg) / (Cd * A * ρ)) - (2 * P) / (Cm * Cd * A * ρ) = 0
     *
     * Then, using the cardanic formulae, the following solutions are found:
     *
     * a = (W^3 - Crvn^3) / 27 - (W * (5 * W * Crvn + (8 * Crvn^2) / (Cd * A * ρ) - 6 * Frg)) / (9 * Cd * A * ρ) + (2 * Frg * Crvn) / (3 * (Cd * A * ρ)^2) + P / (Cm * Cd * A * ρ)
     * b = (2 / (9 * Cd * A * ρ)) * (3 * Frg - 4 * W * Crvn - (W^2) * Cd * A * ρ/2 - (2 * Crvn) / (Cd * A * ρ))
     *
     * If a^2 + b^3 ≥ 0:
     * V = cbrt(a + sqrt(a^2 + b^3)) + cbrt(a - sqrt(a^2 + b^3)) - (2 / 3) * (W + Crvn / (Cd * A * ρ))
     *
     * If a^2 + b^3 < 0:
     * V = 2 * sqrt(-b) * cos((1 / 3) * arccos(a / sqrt(-b^3)) - (2 / 3) * (W + Crvn / (Cd * A * ρ))
     *
     * Although it could be possible to try to estimate wind speed using weather data, for now wind speed is assumed to
     * be 0. Therefore, the above equations can be changed to exlude the parts where the wind speed being 0 would cause
     * various components to no longer be needed. Thus these solutions are used:
     *
     * a = (- Crvn^3) / 27 + (2 * Frg * Crvn) / (3 * (Cd * A * ρ)^2) + P / (Cm * Cd * A * ρ)
     * b = (2 / (9 * Cd * A * ρ)) * (3 * Frg - (2 * Crvn) / (Cd * A * ρ))
     *
     * If a^2 + b^3 ≥ 0:
     * V = cbrt(a + sqrt(a^2 + b^3)) + cbrt(a - sqrt(a^2 + b^3)) - (2 / 3) * (Crvn / (Cd * A * ρ))
     *
     * If a^2 + b^3 < 0:
     * V = 2 * sqrt(-b) * cos((1 / 3) * arccos(a / sqrt(-b^3)) - (2 / 3) * (Crvn / (Cd * A * ρ))
     *
     * @param watts The sustained power output in watts. This represents the value of `P` in the above equations.
     * @param weight The total weight required to be moved that includes the rider(s), their belongings and the vehicle
     *               weight.
     * @param beta ("beta") Inclination angle, = arctan(grade/100). It's probably not a huge time savings and would use
     *              more memory, but additional precalculations from this value could be made. This value is used to
     *              calculate the values of `Crvn` and `Frg` as noted in the above equations.
     * @param coefficientOfRollingResistance The coefficient of rolling resistance. This can also be used to model the
     *              difficulty of traveling over bumpy roadways. This value is used to calculate the value of `Frg` as
     *              noted in the above equations.See this wikipedia page for a list of coefficients by various surface
     *              types: https://en.wikipedia.org/wiki/Rolling_resistance#Rolling_resistance_coefficient_examples
     * @param aerodynamicDragComponent The product of the coefficient of aerodynamic drag, frontal area and air density.
     *              This value is product of (Cd * A * ρ) as noted in the above mathematical equations.
     * @param minSpeed The minimum speed that the micromobility should travel at in cases where the slope is so steep
     *              that it would be faster to walk with the vehicle.
     * @param maxSpeed The maximum speed the vehicle can travel at.
     * @return The speed in m/s. This represents the value of `V` in the above mathematical equations.
     */
    public static double calculateMicromobilitySpeed(
        double watts,
        double weight,
        double beta,
        double coefficientOfRollingResistance,
        double aerodynamicDragComponent,
        double minSpeed,
        double maxSpeed
    ) {
        // assume that end-users will not account for drivetrain inefficencies and will use the default power rating of
        // the vehicle. This adjusts the power downward  to account for drivetrain inefficencies. An assumption is also
        // made that due to use in an urban environment the user may not always be traveling with the maximum available
        // sustained power due to traffic, personal perference, etc
        //
        // FIXME: this current implementation assumes that the maximum sustained power is always used. In reality, the
        //  actual wattage outputted likely depends on the desired speed the user wants to travel at and whether the
        //  vehicle (and person if human-power assist is possible) can output enough power to overcome the resistive
        //  forces needed to travel at that desired speed. For example, on steep downhills, the power output could
        //  actually be negative (ie the user is braking the vehicle). And on the flats, the maximum power output of
        //  some vehicles likely isn't necessary to maintain the desired speed. For now this maxSpeed acts as a good cap
        //  on speeds, but perhaps some more advanced calculation of the actual power could be done. And in turn the
        //  actual power could be used to determine how much fuel has been burned in the vehicle.
        watts = watts * 0.8;

        // The coefficient for the dynamic rolling resistance, normalized to road inclination.
        // In the above mathematical equations, this is the value of `Crvn`.
        // This value could be precalculated during graph build.
        double dynamicRollingResistance = ElevationUtils.getDynamicRollingResistance(beta);

        // Rolling friction (normalized on inclined plane) plus slope pulling force on inclined plane
        // In the above mathematical equations, this is the value of `Frg`.
        double normalizedRollingFriction = ElevationUtils.GRAVITATIONAL_ACCELERATION_CONSTANT *
            weight *
            // These cosine and sine calculations could be precalculated during graph build
            (coefficientOfRollingResistance * Math.cos(beta) + Math.sin(beta));

        double a = (
            -Math.pow(dynamicRollingResistance, 3) / 27.0
        ) + (
            (2.0 * normalizedRollingFriction * dynamicRollingResistance) /
                (3.0 * Math.pow(aerodynamicDragComponent, 2))
        ) + (
            watts / aerodynamicDragComponent
        );
        double b = (
            2.0 / (9.0 * aerodynamicDragComponent)
        ) * (
            3.0 * normalizedRollingFriction -
                (
                    (2.0 * dynamicRollingResistance) / aerodynamicDragComponent
                )
        );

        double cardanicCheck = Math.pow(a, 2) + Math.pow(b, 3);
        double rollingDragComponent = 2.0 / 3.0 * dynamicRollingResistance / aerodynamicDragComponent;
        double speed;
        if (cardanicCheck >= 0) {
            double cardanicCheckSqrt = Math.sqrt(cardanicCheck);
            speed = Math.cbrt(a + cardanicCheckSqrt) +
                Math.cbrt(a - cardanicCheckSqrt) -
                rollingDragComponent;
        } else {
            speed = 2.0 *
                Math.sqrt(-b) *
                Math.cos(1.0 / 3.0 * Math.acos(a / Math.sqrt(Math.pow(-b, 3)))) -
                rollingDragComponent;
        }

        // on steep uphills, the calculated velocity could be slower than the minimum speed. Use the minimum speed in
        // that case.
        speed = Math.max(
            // on steep downhills, the calculated velocity can easily be faster than the max vehicle speed, so cap the
            // speed at the given maximum speed
            Math.min(
                speed,
                maxSpeed
            ),
            minSpeed
        );

        return speed;
    }

    public double getSlopeSpeedEffectiveLength() {
        return getDistance();
    }

    public double getSlopeWorkCostEffectiveLength() {
        return getDistance();
    }

    public double getSlopeWalkSpeedEffectiveLength() {
        return getDistance();
    }

    public void setBicycleSafetyFactor(float bicycleSafetyFactor) {
        this.bicycleSafetyFactor = bicycleSafetyFactor;
    }

    public float getBicycleSafetyFactor() {
        return bicycleSafetyFactor;
    }

    public void setWalkComfortScore(float walkComfortScore) {
        this.walkComfortScore = walkComfortScore;
    }

    public float getWalkComfortScore() {
        return walkComfortScore;
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

    public void setCarNetworks(Set<String> networks) { carNetworks = networks; }

    public Set<String> getCarNetworks() { return carNetworks; }

    public void setVehicleNetworks(Set<String> networks) { vehicleNetworks = networks; }

    public Set<String> getVehicleNetworks() { return vehicleNetworks; }

    public void setTNCStopSuitability(boolean isSuitable) {
        this.suitableForTNCStop = isSuitable;
    }

    public boolean getTNCStopSuitability() { return suitableForTNCStop; }

    public void setFloatingCarDropoffSuitability(boolean isSuitable) {
        this.suitableForFloatingCarRentalDropoff = isSuitable;
    }

    public boolean getFloatingCarDropoffSuitability() { return suitableForFloatingCarRentalDropoff; }

    public void setFloatingVehicleDropoffSuitability(boolean isSuitable) {
        this.suitableForFloatingVehicleRentalDropoff = isSuitable;
    }

    public boolean getFloatingVehicleDropoffSuitability() { return suitableForFloatingVehicleRentalDropoff; }

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

    /**
     * Split this street edge and return the resulting street edges.  There are 3 possible types of splits:
     * 1. A destructive split where two new StreetEdges are created from the existing edge. The edge that was split
     *      has its reference removed from its from and to vertices in the StreetSplitter class thus permanently
     *      removing the possibility of the original edge being traversed in subsequent searches. This split method
     *      should only be used when building a graph. Another important note is that in destructive splits, regular
     *      StreetEdges are created without elevation data, but typically these destructive splits occur in graph
     *      build modules that occur before adding in elevation data.
     * 2. A semi-permanent split is typically used in an updater to create vertices and edges that are specifically for
     *      a single rental car/bike/vehicle station. Further splits of semi-permanent edges are only allowed for
     *      linking the origin and destination for the graph. See {@link SemiPermanentPartialStreetEdge.split}. The
     *      original edge is still traversable in the graph.
     * 3. A temporary split is used to link a StreetEdge to an origin or destination. In this case there is only a need
     *      for creating outgoing edges from the origin and incoming edges to the destination.
     *      // TODO: there is also something about half edges which may not be needed anymore?
     *
     * @param splitterVertex The new vertex that the newly split edge(s) will become connected to.
     * @param destructive Whether or not the split should be permanent and result in the old edge no longer existing
     *                    within the graph.
     * @param createSemiPermanentEdges Whether or not the split should result in the creation of semi-permanent edges
     *                                 or temporary edges.
     */
    public P2<StreetEdge> split(SplitterVertex splitterVertex, boolean destructive, boolean createSemiPermanentEdges) {
        P2<LineString> geoms = GeometryUtils.splitGeometryAtPoint(getGeometry(), splitterVertex.getCoordinate());

        StreetEdge e1 = null;
        StreetEdge e2 = null;

        if (destructive) {
            e1 = new StreetEdge((StreetVertex) fromv, splitterVertex, geoms.first, name, 0, permission, this.isBack());
            e2 = new StreetEdge(splitterVertex, (StreetVertex) tov, geoms.second, name, 0, permission, this.isBack());

            // copy the wayId to the split edges, so we can trace them back to their parent if need be
            e1.wayId = this.wayId;
            e2.wayId = this.wayId;

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

            // TODO: better handle this temporary fix to handle bad edge distance calculation
            if (e1.length_mm < 0) {
                LOG.error("Edge 1 ({}) split at vertex at {},{} has length {} mm. Setting to 1 mm.", e1.wayId, splitterVertex.getLat(), splitterVertex.getLon(), e1.length_mm);
                e1.length_mm = 1;
            }
            if (e2.length_mm < 0) {
                LOG.error("Edge 2 ({}) split at vertex at {},{}  has length {} mm. Setting to 1 mm.", e2.wayId, splitterVertex.getLat(), splitterVertex.getLon(), e2.length_mm);
                e2.length_mm = 1;
            }

            if (e1.length_mm < 0 || e2.length_mm < 0) {
                e1.tov.removeIncoming(e1);
                e1.fromv.removeOutgoing(e1);
                e2.tov.removeIncoming(e2);
                e2.fromv.removeOutgoing(e2);
                throw new IllegalStateException("Split street is longer than original street!");
            }
        } else {
            if (createSemiPermanentEdges) {
                e1 = new SemiPermanentPartialStreetEdge(this, (StreetVertex) fromv, splitterVertex, geoms.first, name);
                e2 = new SemiPermanentPartialStreetEdge(this, splitterVertex, (StreetVertex) tov, geoms.second, name);
            } else {
                boolean splitAtEndVertex = ((TemporarySplitterVertex) splitterVertex).isEndVertex();
                // The StreetVertex of the edge to be split that won't be used when creating a TemporaryPartialStreetEdge
                StreetVertex ununsedExistingStreetVertex = (StreetVertex) (splitAtEndVertex ? tov : fromv);
                if (
                    this instanceof SemiPermanentPartialStreetEdge &&
                        ununsedExistingStreetVertex instanceof SemiPermanentSplitterVertex
                ) {
                    // There is no need to split a SemiPermanentPartialStreetEdge when the splitter vertex is a
                    // SemiPermanentSplitterVertex. The original edge that the SemiPermanentPartialStreetEdge was split
                    // from will also be split and we'd end up with basically 2 identical TemporaryPartialStreetEdges.
                    //
                    // This situation will arise in the following context:
                    //
                    // Given the following graph...
                    //
                    // B =====(1)==== A
                    // \\           // \
                    //   =(3)=D=(4)=   (2)
                    //      ||||        \
                    //       (5)         C
                    //      ||||
                    //        E<(6)>
                    //
                    // Vertices:
                    // A = StreetVertex A
                    // B = StreetVertex B
                    // C = StreetVertex C
                    // D = SemiPermanentSplitterVertices D1 and D2. There will be two of these, one for the split
                    //      between edge A->B and another for the split between edge B->A
                    // E = BikeRentalStationVertex E
                    //
                    // Edges:
                    // 1 = StreetEdges A->B and B->A
                    // 2 = StreetEdge A->C
                    // 3 = SemiPermanentPartialStreetEdges (B->D1 and D2->B) (split from A->B and B->A)
                    // 4 = SemiPermanentPartialStreetEdges (A->D2 and D1->A) (split from A->B and B->A)
                    // 5 = StreetBikeRentalLinks (
                    //          SemiPermanentSplitterVertex D1&2 -> BikeRentalStationVertex E
                    //          and
                    //          BikeRentalStationVertex E -> SemiPermanentSplitterVertex D1&2
                    //     )
                    // 6 = RentABikeOnEdge (E -> E)
                    //
                    // ... assume a request is made to split between vertices B and A at a position that is also between
                    // vertices B and D. The splitting logic will find that the following edges could be split: B->A,
                    // A->B, B->D1 and D2->B. Since B->D1 and D2->B are edges that were already split from B->A and A->B
                    // respectively, the split from B->D1 would be identical to the proposed split from B->A. Therefore,
                    // to avoid creating unneeded edges, the creation of this edge is avoided.
                    LOG.debug("Skipping creation of duplicate TemporaryPartialStreetEdge");
                } else {
                    if (splitAtEndVertex) {
                        e1 = new TemporaryPartialStreetEdge(this, (StreetVertex) fromv, splitterVertex, geoms.first, name, 0);
                    } else {
                        e2 = new TemporaryPartialStreetEdge(this, splitterVertex, (StreetVertex) tov, geoms.second, name, 0);
                    }
                }
            }
        }

        for (StreetEdge e : new StreetEdge[] { e1, e2 }) {
            if (e == null) continue;
            e.copyAttributes(this);
        }

        return new P2<StreetEdge>(e1, e2);
    }

    /**
     * Copies over all attirubtes related to the street (not stuff like geometry and vertices and ids) from an other
     * StreetEdge to this StreetEdge
     */
    private void copyAttributes(StreetEdge other) {
        this.wayId = other.wayId;
        this.setBicycleSafetyFactor(other.getBicycleSafetyFactor());
        this.setWalkComfortScore(other.getWalkComfortScore());
        this.setHasBogusName(other.hasBogusName());
        this.setStairs(other.isStairs());
        this.setWheelchairAccessible(other.isWheelchairAccessible());
        this.setCarNetworks(other.getCarNetworks());
        this.setCarSpeed(other.getCarSpeed());
        this.setFloatingCarDropoffSuitability(other.getFloatingCarDropoffSuitability());
        this.setVehicleNetworks(other.getVehicleNetworks());
        this.setFloatingVehicleDropoffSuitability(other.getFloatingVehicleDropoffSuitability());
        this.setNoThruTraffic(other.isNoThruTraffic());
        this.setStreetClass(other.getStreetClass());
        this.setTNCStopSuitability(other.getTNCStopSuitability());
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

    public Map<String, String> getOsmTags() {
        return osmTags;
    }

    public void setOsmTags(Map<String, String> osmTags) {
        this.osmTags = osmTags;
    }

    public boolean addCarNetwork(String carNetwork) {
        if (carNetworks == null) {
            synchronized (this) {
                if (carNetworks == null) {
                    carNetworks = new HashSet<>();
                }
            }
        }
        return carNetworks.add(carNetwork);
    }

    public boolean containsCarNetwork(String carNetwork) {
        if (carNetworks == null){
            return false;
        }
        return carNetworks.contains(carNetwork);
    }

    public boolean addVehicleNetwork(String vehicleNetwork) {
        if (vehicleNetworks == null) {
            vehicleNetworks = new HashSet<>();
        }
        return vehicleNetworks.add(vehicleNetwork);
    }

    public boolean containsVehicleNetwork(String vehicleNetwork) {
        if (vehicleNetworks == null){
            return false;
        }
        return vehicleNetworks.contains(vehicleNetwork);
    }
}
