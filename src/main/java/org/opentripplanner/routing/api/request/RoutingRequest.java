package org.opentripplanner.routing.api.request;

import org.opentripplanner.api.common.LocationStringParser;
import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.core.IntersectionTraversalCostModel;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.SimpleIntersectionTraversalCostModel;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.error.TrivialPathException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DurationComparator;
import org.opentripplanner.routing.impl.PathComparator;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * A trip planning request. Some parameters may not be honored by the trip planner for some or all
 * itineraries. For example, maxWalkDistance may be relaxed if the alternative is to not provide a
 * route.
 *
 * All defaults should be specified here in the RoutingRequest, NOT as annotations on query
 * parameters in web services that create RoutingRequests. This establishes a priority chain for
 * default values:
 * RoutingRequest field initializers, then JSON router config, then query parameters.
 *
 * @Deprecated tag is added to all parameters that are not currently functional in either the Raptor router or other
 * non-transit routing (walk, bike, car etc.)
 *
 * TODO OTP2 Many fields are deprecated in this class, the reason is documented in the
 *           RoutingResource class, not here. Eventually the field will be removed from this
 *           class, but we want to keep it in the RoutingResource as long as we support the
 *           REST API.
 */
public class RoutingRequest implements Cloneable, Serializable {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    private static final Logger LOG = LoggerFactory.getLogger(RoutingRequest.class);

    /**
     * The model that computes turn/traversal costs.
     * TODO: move this to the Router or the Graph if it doesn't clutter the code too much
     */
    public IntersectionTraversalCostModel traversalCostModel = new SimpleIntersectionTraversalCostModel();

    /* FIELDS UNIQUELY IDENTIFYING AN SPT REQUEST */

    /** The complete list of incoming query parameters. */
    public final HashMap<String, String> parameters = new HashMap<String, String>();

    /** The start location */
    public GenericLocation from;

    /** The end location */
    public GenericLocation to;

    /**
     * If true, the tree will be allowed to grow in all directions, rather than being directed
     * toward a single target. This parameter only apply to access/egress AStar searches,
     * not transit searches in Raptor.
     *
     * @deprecated TODO OTP2 - This looks like an A Star implementation detail. Should be moved to
     *                       - an A Star specific request class
     */
    @Deprecated
    public boolean oneToMany = false;

    /**
     * An ordered list of intermediate locations to be visited.
     *
     * @deprecated TODO OTP2 - Regression. Not currently working in OTP2. Must be re-implemented
     *                       - using raptor.
     */
    @Deprecated
    public List<GenericLocation> intermediatePlaces;

    /**
     * The maximum distance (in meters) the user is willing to walk for access/egress legs.
     * Defaults to unlimited.
     *
     * @deprecated TODO OTP2 Regression. Not currently working in OTP2. We might not implement the
     *                       old functionality the same way, but we will try to map this parameter
     *                       so it does work similar as before.
     * @see https://github.com/opentripplanner/OpenTripPlanner/issues/2886
     */
    @Deprecated
    public double maxWalkDistance = Double.MAX_VALUE;

    /**
     * The maximum distance (in meters) the user is willing to walk for transfer legs.
     * Defaults to unlimited. Currently set to be the same value as maxWalkDistance.
     *
     * @Deprecated TODO OTP2 This is replaced by a similar build parameter. This parameter do
     *                       not exist in the REST API - so it can be removed safely from here.
     */
    @Deprecated
    public double maxTransferWalkDistance = Double.MAX_VALUE;

    /**
     * The maximum time (in seconds) of pre-transit travel when using drive-to-transit (park and
     * ride or kiss and ride). By default limited to 30 minutes driving, because if it's unlimited on
     * large graphs the search becomes very slow.
     *
     * @deprecated TODO OTP2 - Regression. Not currently working in OTP2.
     * @see https://github.com/opentripplanner/OpenTripPlanner/issues/2886
     */
    @Deprecated
    public int maxPreTransitTime = 30 * 60;

    /**
     * The worst possible time (latest for depart-by and earliest for arrive-by) to accept
     *
     * @Deprecated TODO OTP2 This is a parameter specific to the AStar and work as a cut-off.
     *                       Raptor have a similar concept, the search window. This parameter
     *                       do not belong in the request object, is should be pushed down into
     *                       AStar and then we need to find a way to resolve the search time
     *                       window. There is more than one strategy for this.
     */
    @Deprecated
    public long worstTime = Long.MAX_VALUE;

    /**
     * The worst possible weight that we will accept when planning a trip.
     *
     * @deprecated TODO OTP2 This is not in use, and sub-optimal to prune a search on. It should
     *                       be removed.
     */
    @Deprecated
    public double maxWeight = Double.MAX_VALUE;

    /**
     * The maximum duration of a returned itinerary, in hours.
     *
     * @deprecated TODO OTP2 This is not useful as a search parameter, but could be used as a
     *                       post search filter to reduce number of itineraries down to an
     *                       acceptable number, but there are probably better ways to do that.
     */
    @Deprecated
    public double maxHours = Double.MAX_VALUE;

    /**
     * Whether maxHours limit should consider wait/idle time between the itinerary and the
     * requested arrive/depart time.
     *
     * @deprecated see {@link #maxHours}
     */
    @Deprecated
    public boolean useRequestedDateTimeInMaxHours = false;

    /**
     * The access/egress/direct/transit modes allowed for this main request. The parameter
     * "streetSubRequestModes" below is used for a single A Star sub request.
     *
     * // TODO OTP2 Street routing requests should eventually be split into its own request class.
     */
    public RequestModes modes = new RequestModes(
        StreetMode.WALK,
        StreetMode.WALK,
        StreetMode.WALK,
        Collections.emptySet()
    );

    /**
     * The set of TraverseModes allowed when doing creating sub requests and doing street routing.
     * // TODO OTP2 Street routing requests should eventually be split into its own request class.
     */
    public TraverseModeSet streetSubRequestModes = new TraverseModeSet(TraverseMode.WALK); // defaults in constructor overwrite this

    /**
     * The set of characteristics that the user wants to optimize for -- defaults to QUICK, or
     * optimize for transit time.
     *
     * @deprecated TODO OTP2 this should be completely removed and done only with individual cost
     *                       parameters
     *                       Also: apparently OptimizeType only affects BICYCLE mode traversal of
     *                       street segments. If this is the case it should be very well
     *                       documented and carried over into the Enum name.
     */
    @Deprecated
    public OptimizeType optimize = OptimizeType.QUICK;

    /** The epoch date/time that the trip should depart (or arrive, for requests where arriveBy is true) */
    public long dateTime = new Date().getTime() / 1000;

    /**
     * This is the time/duration in seconds from the earliest-departure-time(EDT) to
     * latest-departure-time(LDT). In case of a reverse search it will be the time from earliest
     * to latest arrival time (LAT - EAT).
     * <p>
     * All optimal travels that depart within the search window is guarantied to be found.
     * <p>
     * This is sometimes referred to as the Range Raptor Search Window - but could be used in a none
     * Transit search as well; Hence this is named search-window and not raptor-search-window. Do
     * not confuse this with the travel-window, which is the time between EDT to LAT.
     * <p>
     * Use {@code null} to unset, and {@link Duration#ZERO} to do one Raptor iteration. The value is
     * dynamically  assigned a suitable value, if not set. In a small to medium size operation
     * you may use a fixed value, like 60 minutes. If you have a mixture of high frequency cities
     * routes and infrequent long distant journeys, the best option is normally to use the dynamic
     * auto assignment.
     */
    public Duration searchWindow;

    /**
     * Whether the trip should depart at dateTime (false, the default), or arrive at dateTime.
     */
    public boolean arriveBy = false;

    /**
     * Whether the trip must be wheelchair accessible.
     * @deprecated TODO OTP2 Regression. This is not implemented in Raptor yet, but will work with
     *                 a walk-only search.
     */
    @Deprecated
    public boolean wheelchairAccessible = false;

    /** The maximum number of itineraries to return. */
    public int numItineraries = 3;

    /** The maximum slope of streets for wheelchair trips. */
    public double maxWheelchairSlope = 0.0833333333333; // ADA max wheelchair ramp slope is a good default.

    /** Whether the planner should return intermediate stops lists for transit legs. */
    // TODO OTP2 Maybe this should be up to the API?
    public boolean showIntermediateStops = false;

    /** max walk/bike speed along streets, in meters per second */
    public double walkSpeed;

    public double bikeSpeed;

    public double carSpeed;

    public Locale locale = new Locale("en", "US");

    /**
     * An extra penalty added on transfers (i.e. all boardings except the first one).
     * Not to be confused with bikeBoardCost and walkBoardCost, which are the cost of boarding a
     * vehicle with and without a bicycle. The boardCosts are used to model the 'usual' perceived
     * cost of using a transit vehicle, and the transferPenalty is used when a user requests even
     * less transfers. In the latter case, we don't actually optimize for fewest transfers, as this
     * can lead to absurd results. Consider a trip in New York from Grand Army
     * Plaza (the one in Brooklyn) to Kalustyan's at noon. The true lowest transfers route is to
     * wait until midnight, when the 4 train runs local the whole way. The actual fastest route is
     * the 2/3 to the 4/5 at Nevins to the 6 at Union Square, which takes half an hour.
     * Even someone optimizing for fewest transfers doesn't want to wait until midnight. Maybe they
     * would be willing to walk to 7th Ave and take the Q to Union Square, then transfer to the 6.
     * If this takes less than optimize_transfer_penalty seconds, then that's what we'll return.
     *
     * @deprecated TODO OTP2 Regression. Not currently working in OTP2. We might not implement the
     *                       old functionality the same way, but we will try to map this parameter
     *                       so it does work similar as before.
     */
    @Deprecated
    public int transferCost = 0;

    /**
     * Penalty for using a non-preferred transfer
     *
     * @deprecated TODO OTP2 Regression. Not currently working in OTP2. We might not implement the
     *                       old functionality the same way, but we will try to map this parameter
     *                       so it does work similar as before.
     */
    @Deprecated
    public int nonpreferredTransferCost = 180;

    /** A multiplier for how bad walking is, compared to being in transit for equal lengths of time.
     *  Defaults to 2. Empirically, values between 10 and 20 seem to correspond well to the concept
     *  of not wanting to walk too much without asking for totally ridiculous itineraries, but this
     *  observation should in no way be taken as scientific or definitive. Your mileage may vary.
     */
    public double walkReluctance = 2.0;

    /** Used instead of walk reluctance for stairs */
    public double stairsReluctance = 2.0;

    /** Multiplicative factor on expected turning time. */
    public double turnReluctance = 1.0;

    /**
     * How long does it take to get an elevator, on average (actually, it probably should be a bit *more* than average, to prevent optimistic trips)?
     * Setting it to "seems like forever," while accurate, will probably prevent OTP from working correctly.
     */
    // TODO: how long does it /really/ take to get an elevator?
    public int elevatorBoardTime = 90;

    /** What is the cost of boarding an elevator? */
    public int elevatorBoardCost = 90;

    /** How long does it take to advance one floor on an elevator? */
    public int elevatorHopTime = 20;

    /** What is the cost of travelling one floor on an elevator? */
    public int elevatorHopCost = 20;

    // it is assumed that getting off an elevator is completely free

    /** Time to get on and off your own bike */
    public int bikeSwitchTime;

    /** Cost of getting on and off your own bike */
    public int bikeSwitchCost;

    /** Time to rent a bike */
    public int bikeRentalPickupTime = 60;

    /**
     * Cost of renting a bike. The cost is a bit more than actual time to model the associated cost and trouble.
     */
    public int bikeRentalPickupCost = 120;

    /** Time to drop-off a rented bike */
    public int bikeRentalDropoffTime = 30;

    /** Cost of dropping-off a rented bike */
    public int bikeRentalDropoffCost = 30;

    /** Time to park a bike */
    public int bikeParkTime = 60;

    /** Cost of parking a bike. */
    public int bikeParkCost = 120;

    /**
     * Time to park a car in a park and ride, w/o taking into account driving and walking cost
     * (time to park, switch off, pick your stuff, lock the car, etc...)
     */
    public int carDropoffTime = 120;

    /**
     * How much worse is waiting for a transit vehicle than being on a transit vehicle, as a multiplier. The default value treats wait and on-vehicle
     * time as the same.
     *
     * It may be tempting to set this higher than walkReluctance (as studies often find this kind of preferences among
     * riders) but the planner will take this literally and walk down a transit line to avoid waiting at a stop.
     * This used to be set less than 1 (0.95) which would make waiting offboard preferable to waiting onboard in an
     * interlined trip. That is also undesirable.
     *
     * If we only tried the shortest possible transfer at each stop to neighboring stop patterns, this problem could disappear.
     */
    public double waitReluctance = 1.0;

    /** How much less bad is waiting at the beginning of the trip (replaces waitReluctance on the first boarding)
     *
     * @deprecated TODO OTP2 Probably a regression, but I'm not sure it worked correctly in OTP 1.X
 *                          either. It could be a part of itinerary-filtering after a Raptor search.
     * */
    @Deprecated
    public double waitAtBeginningFactor = 0.4;

    /** This prevents unnecessary transfers by adding a cost for boarding a vehicle.
     *
     * @Deprecated TODO OTP2 - Regression. Could be implemented as a part of itinerary-filtering
     *                          after a Raptor search.
     * */
    @Deprecated
    public int walkBoardCost = 60 * 10;

    /** Separate cost for boarding a vehicle with a bicycle, which is more difficult than on foot.
     *
     * @Deprecated TODO OTP2 - Regression. Could be implemented as a part of itinerary-filtering
     *                          after a Raptor search.
     * */
    @Deprecated
    public int bikeBoardCost = 60 * 10;

    /**
     * Do not use certain named agencies
     */
    public HashSet<FeedScopedId> bannedAgencies = new HashSet<>();

    /**
     * Only use certain named agencies
     */
    public HashSet<FeedScopedId> whiteListedAgencies = new HashSet<>();


    /**
     * Set of preferred agencies by user.
     *
     * @deprecated TODO OTP2: Needs to be implemented
     */
    @Deprecated
    public HashSet<FeedScopedId> preferredAgencies = new HashSet<>();

    /**
     * Set of unpreferred agencies for given user.
     */
    @Deprecated
    public HashSet<FeedScopedId> unpreferredAgencies = new HashSet<>();

    /**
     * Do not use certain named routes.
     * The paramter format is: feedId_routeId,feedId_routeId,feedId_routeId
     * This parameter format is completely nonstandard and should be revised for the 2.0 API, see issue #1671.
     */
    public RouteMatcher bannedRoutes = RouteMatcher.emptyMatcher();

    /** Only use certain named routes
     */
    public RouteMatcher whiteListedRoutes = RouteMatcher.emptyMatcher();

    /** Set of preferred routes by user.
     *
     * @deprecated TODO OTP2 Needs to be implemented
     */
    @Deprecated
    public RouteMatcher preferredRoutes = RouteMatcher.emptyMatcher();

    /**
     * Penalty added for using every route that is not preferred if user set any route as preferred.
     * We return number of seconds that we are willing to wait for preferred route.
     *
     * @deprecated TODO OTP2 Needs to be implemented
     */
    @Deprecated
    public int otherThanPreferredRoutesPenalty = 300;

    /**
     * Set of unpreferred routes for given user.
     *
     * @deprecated TODO OTP2: Needs to be implemented
     */
    @Deprecated
    public RouteMatcher unpreferredRoutes = RouteMatcher.emptyMatcher();

    /**
     * Penalty added for using every unpreferred route. We return number of seconds that we are
     * willing to wait for preferred route.
     *
     * @deprecated TODO OTP2: Needs to be implemented
     */
    @Deprecated
    public int useUnpreferredRoutesPenalty = 300;

    /**
     * Do not use certain trips
     *
     * @deprecated TODO OTP2: Needs to be implemented
     */
    @Deprecated
    public HashMap<FeedScopedId, BannedStopSet> bannedTrips = new HashMap<FeedScopedId, BannedStopSet>();

    /**
     * A global minimum transfer time (in seconds) that specifies the minimum amount of time that
     * must pass between exiting one transit vehicle and boarding another. This time is in addition
     * to time it might take to walk between transit stops, the {@link #alightSlack}, and the
     * {@link #boardSlack}. This time should also be overridden by specific transfer timing
     * information in transfers.txt
     * <p>
     * This only apply to transfers between two trips, it does not apply when boarding the first
     * transit.
     * <p>
     * Unit is seconds. Default value is 2 minutes.
     */
    public int transferSlack = 120;

    /**
     * The number of seconds to add before boarding a transit leg. It is recommended to use the
     * `boardTimes` in the `router-config.json` to set this for each mode.
     * <p>
     * Unit is seconds. Default value is 0.
     */
    public int boardSlack = 0;

    /**
     * Has information how much time boarding a vehicle takes. Can be significant eg in airplanes
     * or ferries.
     * <p>
     * If set, the board-slack-for-mode override the more general {@link #boardSlack}. This
     * enables configuring the board-slack for airplane boarding to be 30 minutes and a slack
     * for bus of 2 minutes.
     * <p>
     * Unit is seconds. Default value is not-set(empty map).
     */
    public Map<TraverseMode, Integer> boardSlackForMode = new HashMap<>();

    /**
     * The number of seconds to add after alighting a transit leg. It is recommended to use the
     * `alightTimes` in the `router-config.json` to set this for each mode.
     * <p>
     * Unit is seconds. Default value is 0.
     */
    public int alightSlack = 0;

    /**
     * Has information how much time alighting a vehicle takes. Can be significant eg in airplanes
     * or ferries.
     * <p>
     * If set, the alight-slack-for-mode override the more general {@link #alightSlack}. This
     * enables configuring the alight-slack for train alighting to be 4 minutes and a bus alight
     * slack to be 0 minutes.
     * <p>
     * Unit is seconds. Default value is not-set(empty map).
     */
    public Map<TraverseMode, Integer> alightSlackForMode = new HashMap<>();

    /**
     * Ideally maxTransfers should be set in the router config, not here. Instead the client should
     * be able to pass in a parameter for the max number of additional/extra transfers relative to
     * the best trip (with the fewest possible transfers) within constraint of the other search
     * parameters(TODO OTP2 Expose {@link org.opentripplanner.transit.raptor.api.request.SearchParams#numberOfAdditionalTransfers()}
     * in APIs). This might be to complicated to explain to the customer, so we might stick to the
     * old limit, but that have side-effects that you might not find any trips on a day where a
     * critical part of the trip is not available, because of some real-time disruption.
     *
     * @see https://github.com/opentripplanner/OpenTripPlanner/issues/2886
     */
    public Integer maxTransfers = null;

    /**
     * For the bike triangle, how important time is.
     * triangleTimeFactor+triangleSlopeFactor+triangleSafetyFactor == 1
     */
    public double bikeTriangleTimeFactor;

    /** For the bike triangle, how important slope is */
    public double bikeTriangleSlopeFactor;

    /** For the bike triangle, how important safety is */
    public double bikeTriangleSafetyFactor;

    /** Options specifically for the case that you are walking a bicycle. */
    public RoutingRequest bikeWalkingOptions;

    /**
     * Whether or not bike rental availability information will be used to plan bike rental trips
     */
    public boolean useBikeRentalAvailabilityInformation = false;

    /**
     * If true, cost turns as they would be in a country where driving occurs on the right; otherwise, cost them as they would be in a country where
     * driving occurs on the left.
     */
    public boolean driveOnRight = true;

    /**
     * The deceleration speed of an automobile, in meters per second per second.
     */
    // 2.9 m/s/s: 65 mph - 0 mph in 10 seconds
    public double carDecelerationSpeed = 2.9;

    /**
     * The acceleration speed of an automobile, in meters per second per second.
     */
    // 2.9 m/s/s: 0 mph to 65 mph in 10 seconds
    public double carAccelerationSpeed = 2.9;

    /**
     * When true, realtime updates are ignored during this search.
     */
    public boolean ignoreRealtimeUpdates = false;

    /**
     * If true, the remaining weight heuristic is disabled. Currently only implemented for the long
     * distance path service.
     *
     * This is used by the Street search only.
     *
     * TODO OTP2 Can we merge this with the 'oneToMany' option?
     */
    public boolean disableRemainingWeightHeuristic = false;

    /**
     * The routing context used to actually carry out this search. It is important to build States from TraverseOptions
     * rather than RoutingContexts,and just keep a reference to the context in the TraverseOptions, rather than using
     * RoutingContexts for everything because in some testing and graph building situations we need to build a bunch of
     * initial states with different times and vertices from a single TraverseOptions, without setting all the transit
     * context or building temporary vertices (with all the exception-throwing checks that entails).
     *
     * While they are conceptually separate, TraverseOptions does maintain a reference to its accompanying
     * RoutingContext (and vice versa) so that both do not need to be passed/injected separately into tight inner loops
     * within routing algorithms. These references should be set to null when the request scope is torn down -- the
     * routing context becomes irrelevant at that point, since temporary graph elements have been removed and the graph
     * may have been reloaded.
     */
    public RoutingContext rctx;

    /**
     * A transit stop that this trip must start from
     *
     * @deprecated TODO OTP2 Is this in use, what is is used for. It seems to overlap with
     *                       the fromPlace parameter. Is is used for onBoard routing only?
     */
    @Deprecated
    public FeedScopedId startingTransitStopId;

    /**
     * A trip where this trip must start from (depart-onboard routing)
     *
     * @deprecated TODO OTP2 Regression. Not currently working in OTP2. We might not implement the
     *                       old functionality the same way, but we will try to map this parameter
     *                       so it does work similar as before.
     */
    @Deprecated
    public FeedScopedId startingTransitTripId;

    public boolean walkingBike;

    /*
      Additional flags affecting mode transitions.
      This is a temporary solution, as it only covers parking and rental at the beginning of the trip.
    */
    public boolean bikeRental = false;
    public boolean bikeParkAndRide = false;
    public boolean parkAndRide  = false;
    public boolean carPickup = false;

    /** The function that compares paths converging on the same vertex to decide which ones continue to be explored. */
    public DominanceFunction dominanceFunction = new DominanceFunction.Pareto();

    /**
     * Accept only paths that use transit (no street-only paths).
     *
     * @Deprecated TODO OTP2 Regression. Not currently working in OTP2. This is only used in the
     *                       deprecated Transmodel GraphQL API.
     *
     */
    @Deprecated
    public boolean onlyTransitTrips = false;

    /** Option to disable the default filtering of GTFS-RT alerts by time. */
    public boolean disableAlertFiltering = false;

    /** Whether to apply the ellipsoid→geoid offset to all elevations in the response */
    public boolean geoidElevation = false;

    /** Which path comparator to use
     *
     * @deprecated TODO OTP2 Regression. Not currently working in OTP2 at the moment.
     */
    @Deprecated
    public String pathComparator = null;


    /**
     * Switch on to return all itineraries and mark filtered itineraried as deleted.
     */
    public boolean debugItineraryFilter = false;

    /** Saves split edge which can be split on origin/destination search
     *
     * This is used so that TrivialPathException is thrown if origin and destination search would split the same edge
     */
    public StreetEdge splitEdge = null;

    /* CONSTRUCTORS */

    /** Constructor for options; modes defaults to walk and transit */
    public RoutingRequest() {
        // http://en.wikipedia.org/wiki/Walking
        walkSpeed = 1.33; // 1.33 m/s ~ 3mph, avg. human speed
        bikeSpeed = 5; // 5 m/s, ~11 mph, a random bicycling speed
        // http://en.wikipedia.org/wiki/Speed_limit
        carSpeed = 40; // 40 m/s, 144 km/h, above the maximum (finite) driving speed limit worldwide
        // Default to walk for access/egress/direct modes and all transit modes
        this.modes = new RequestModes(StreetMode.WALK, StreetMode.WALK, StreetMode.WALK, new HashSet<>(
            Arrays.asList(TransitMode.values())));
        bikeWalkingOptions = this;

        // So that they are never null.
        from = new GenericLocation(null, null);
        to = new GenericLocation(null, null);
    }

    public RoutingRequest(TraverseModeSet streetSubRequestModes) {
        this();
        this.setStreetSubRequestModes(streetSubRequestModes);
    }

    public RoutingRequest(TraverseMode mode) {
        this();
        this.setStreetSubRequestModes(new TraverseModeSet(mode));
    }

    public RoutingRequest(TraverseMode mode, OptimizeType optimize) {
        this(new TraverseModeSet(mode), optimize);
    }

    public RoutingRequest(TraverseModeSet modeSet, OptimizeType optimize) {
        this();
        this.optimize = optimize;
        this.setStreetSubRequestModes(modeSet);
    }

    /* ACCESSOR/SETTER METHODS */

    public boolean transitAllowed() {
        return streetSubRequestModes.isTransit();
    }

    public long getSecondsSinceEpoch() {
        return dateTime;
    }

    public void setArriveBy(boolean arriveBy) {
        this.arriveBy = arriveBy;
        bikeWalkingOptions.arriveBy = arriveBy;
        if (worstTime == Long.MAX_VALUE || worstTime == 0)
            worstTime = arriveBy ? 0 : Long.MAX_VALUE;
    }

    public void setMode(TraverseMode mode) {
        setStreetSubRequestModes(new TraverseModeSet(mode));
    }

    public void setStreetSubRequestModes(TraverseModeSet streetSubRequestModes) {
        this.streetSubRequestModes = streetSubRequestModes;
        if (streetSubRequestModes.getBicycle()) {
            // This alternate routing request is used when we get off a bike to take a shortcut and are
            // walking alongside the bike. FIXME why are we only copying certain fields instead of cloning the request?
            bikeWalkingOptions = new RoutingRequest();
            bikeWalkingOptions.setArriveBy(this.arriveBy);
            bikeWalkingOptions.maxWalkDistance = maxWalkDistance;
            bikeWalkingOptions.maxPreTransitTime = maxPreTransitTime;
            bikeWalkingOptions.walkSpeed = walkSpeed * 0.8; // walking bikes is slow
            bikeWalkingOptions.walkReluctance = walkReluctance * 2.7; // and painful
            bikeWalkingOptions.optimize = optimize;
            bikeWalkingOptions.streetSubRequestModes = streetSubRequestModes.clone();
            bikeWalkingOptions.streetSubRequestModes.setBicycle(false);
            bikeWalkingOptions.streetSubRequestModes.setWalk(true);
            bikeWalkingOptions.walkingBike = true;
            bikeWalkingOptions.bikeSwitchTime = bikeSwitchTime;
            bikeWalkingOptions.bikeSwitchCost = bikeSwitchCost;
            bikeWalkingOptions.stairsReluctance = stairsReluctance * 5; // carrying bikes on stairs is awful
        } else if (streetSubRequestModes.getCar()) {
            bikeWalkingOptions = new RoutingRequest();
            bikeWalkingOptions.setArriveBy(this.arriveBy);
            bikeWalkingOptions.maxWalkDistance = maxWalkDistance;
            bikeWalkingOptions.maxPreTransitTime = maxPreTransitTime;
            bikeWalkingOptions.streetSubRequestModes = streetSubRequestModes.clone();
            bikeWalkingOptions.streetSubRequestModes.setBicycle(false);
            bikeWalkingOptions.streetSubRequestModes.setWalk(true);
        }
    }

    public void setOptimize(OptimizeType optimize) {
        this.optimize = optimize;
        bikeWalkingOptions.optimize = optimize;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    /**
     * only allow traversal by the specified mode; don't allow walking bikes. This is used during contraction to reduce the number of possible paths.
     */
    public void freezeTraverseMode() {
        bikeWalkingOptions = clone();
        bikeWalkingOptions.bikeWalkingOptions = new RoutingRequest(new TraverseModeSet());
    }

    /** Returns the model that computes the cost of intersection traversal. */
    public IntersectionTraversalCostModel getIntersectionTraversalCostModel() {
        return traversalCostModel;
    }

    /** @return the (soft) maximum walk distance */
    // If transit is not to be used and this is a point to point search
    // or one with soft walk limiting, disable walk limit.
    public double getMaxWalkDistance() {
        if (streetSubRequestModes.isTransit()) {
            return maxWalkDistance;
        } else {
            return Double.MAX_VALUE;
        }
    }

    public void setWalkBoardCost(int walkBoardCost) {
        if (walkBoardCost < 0) {
            this.walkBoardCost = 0;
        }
        else {
            this.walkBoardCost = walkBoardCost;
        }
    }

    public void setBikeBoardCost(int bikeBoardCost) {
        if (bikeBoardCost < 0) {
            this.bikeBoardCost = 0;
        }
        else {
            this.bikeBoardCost = bikeBoardCost;
        }
    }

    public void setPreferredAgencies(String s) {
        if (!s.isEmpty()) {
            preferredAgencies = FeedScopedId.parseListOfIds(s);
        }
    }

    public void setPreferredRoutes(String s) {
        if (!s.isEmpty()) {
            preferredRoutes = RouteMatcher.parse(s);
        }
        else {
            preferredRoutes = RouteMatcher.emptyMatcher();
        }
    }

    public void setOtherThanPreferredRoutesPenalty(int penalty) {
        if(penalty < 0) penalty = 0;
        this.otherThanPreferredRoutesPenalty = penalty;
    }

    public void setUnpreferredAgencies(String s) {
        if (!s.isEmpty()) {
            unpreferredAgencies = FeedScopedId.parseListOfIds(s);
        }
    }

    public void setUnpreferredRoutes(String s) {
        if (!s.isEmpty()) {
            unpreferredRoutes = RouteMatcher.parse(s);
        }
        else {
            unpreferredRoutes = RouteMatcher.emptyMatcher();
        }
    }

    public void setBannedRoutes(String s) {
        if (!s.isEmpty()) {
            bannedRoutes = RouteMatcher.parse(s);
        }
        else {
            bannedRoutes = RouteMatcher.emptyMatcher();
        }
    }

    public void setWhiteListedRoutes(String s) {
        if (!s.isEmpty()) {
            whiteListedRoutes = RouteMatcher.parse(s);
        }
        else {
            whiteListedRoutes = RouteMatcher.emptyMatcher();
        }
    }

    public void setBannedAgencies(String s) {
        if (!s.isEmpty()) {
            bannedAgencies = FeedScopedId.parseListOfIds(s);
        }
    }

    public void setWhiteListedAgencies(String s) {
        if (!s.isEmpty()) {
            whiteListedAgencies = FeedScopedId.parseListOfIds(s);
        }
    }

    public final static int MIN_SIMILARITY = 1000;

    public void setFromString(String from) {
        this.from = LocationStringParser.fromOldStyleString(from);
    }

    public void setToString(String to) {
        this.to = LocationStringParser.fromOldStyleString(to);
    }

    /**
     * Add a TraverseMode to the set of allowed modes.
     */
    public void addMode(TraverseMode mode) {
        streetSubRequestModes.setMode(mode, true);
    }

    public Date getDateTime() {
        return new Date(dateTime * 1000);
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime.getTime() / 1000;
    }

    public void setDateTime(String date, String time, TimeZone tz) {
        Date dateObject = DateUtils.toDate(date, time, tz);
        setDateTime(dateObject);
    }

    public int getNumItineraries() {
        if (streetSubRequestModes.isTransit()) {
            return numItineraries;
        } else {
            // If transit is not to be used, only search for one itinerary.
            return 1;
        }
    }

    public void setNumItineraries(int numItineraries) {
        this.numItineraries = numItineraries;
    }

    public String toString() {
        return toString(" ");
    }

    public String toString(String sep) {
        return from + sep + to + sep + getMaxWalkDistance() + sep + getDateTime() + sep
                + arriveBy + sep + optimize + sep + streetSubRequestModes.getAsStr() + sep
                + getNumItineraries();
    }

    public void removeMode(TraverseMode mode) {
        streetSubRequestModes.setMode(mode, false);
    }

    /**
     * Sets intermediatePlaces by parsing GenericLocations from a list of string.
     */
    public void setIntermediatePlacesFromStrings(List<String> intermediates) {
        this.intermediatePlaces = new ArrayList<GenericLocation>(intermediates.size());
        for (String place : intermediates) {
            intermediatePlaces.add(LocationStringParser.fromOldStyleString(place));
        }
    }

    /** Clears any intermediate places from this request. */
    public void clearIntermediatePlaces() {
        if (this.intermediatePlaces != null) {
            this.intermediatePlaces.clear();
        }
    }

    /**
     * Returns true if there are any intermediate places set.
     */
    public boolean hasIntermediatePlaces() {
        return this.intermediatePlaces != null && this.intermediatePlaces.size() > 0;
    }

    /**
     * Adds a GenericLocation to the end of the intermediatePlaces list. Will initialize intermediatePlaces if it is null.
     */
    public void addIntermediatePlace(GenericLocation location) {
        if (this.intermediatePlaces == null) {
            this.intermediatePlaces = new ArrayList<GenericLocation>();
        }
        this.intermediatePlaces.add(location);
    }

    public void setBikeTriangleSafetyFactor(double bikeTriangleSafetyFactor) {
        this.bikeTriangleSafetyFactor = bikeTriangleSafetyFactor;
        bikeWalkingOptions.bikeTriangleSafetyFactor = bikeTriangleSafetyFactor;
    }

    public void setBikeTriangleSlopeFactor(double bikeTriangleSlopeFactor) {
        this.bikeTriangleSlopeFactor = bikeTriangleSlopeFactor;
        bikeWalkingOptions.bikeTriangleSlopeFactor = bikeTriangleSlopeFactor;
    }

    public void setBikeTriangleTimeFactor(double bikeTriangleTimeFactor) {
        this.bikeTriangleTimeFactor = bikeTriangleTimeFactor;
        bikeWalkingOptions.bikeTriangleTimeFactor = bikeTriangleTimeFactor;
    }


    /* INSTANCE METHODS */

    public RoutingRequest getStreetSearchRequest(StreetMode streetMode) {
        RoutingRequest streetRequest = this.clone();
        streetRequest.streetSubRequestModes = new TraverseModeSet();

        if (streetMode != null) {
            switch (streetMode) {
                case WALK:
                    streetRequest.streetSubRequestModes.setWalk(true);
                    break;
                case BIKE:
                    streetRequest.streetSubRequestModes.setBicycle(true);
                    break;
                case BIKE_TO_PARK:
                    streetRequest.streetSubRequestModes.setBicycle(true);
                    streetRequest.streetSubRequestModes.setWalk(true);
                    streetRequest.bikeParkAndRide = true;
                    break;
                case BIKE_RENTAL:
                    streetRequest.streetSubRequestModes.setBicycle(true);
                    streetRequest.streetSubRequestModes.setWalk(true);
                    streetRequest.bikeRental = true;
                    break;
                case CAR:
                    streetRequest.streetSubRequestModes.setCar(true);
                    break;
                case CAR_TO_PARK:
                    streetRequest.streetSubRequestModes.setCar(true);
                    streetRequest.streetSubRequestModes.setWalk(true);
                    streetRequest.parkAndRide = true;
                    break;
                case CAR_PICKUP:
                    streetRequest.streetSubRequestModes.setCar(true);
                    streetRequest.streetSubRequestModes.setWalk(true);
                    streetRequest.carPickup = true;
                    break;
                case CAR_RENTAL:
                    streetRequest.streetSubRequestModes.setCar(true);
                    streetRequest.streetSubRequestModes.setWalk(true);
            }
        }

        streetRequest.resetRoutingContext();

        return streetRequest;
    }

    // TODO OTP2 This is needed in order to find the correct from/to vertices for the mode
    private void resetRoutingContext() {
        Graph graph = rctx.graph;
        rctx = null;
        splitEdge = null;
        setRoutingContext(graph);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RoutingRequest clone() {
        try {
            RoutingRequest clone = (RoutingRequest) super.clone();
            clone.streetSubRequestModes = streetSubRequestModes.clone();
            clone.bannedRoutes = bannedRoutes.clone();
            clone.bannedTrips = (HashMap<FeedScopedId, BannedStopSet>) bannedTrips.clone();
            clone.whiteListedAgencies = (HashSet<FeedScopedId>) whiteListedAgencies.clone();
            clone.whiteListedRoutes = whiteListedRoutes.clone();
            clone.preferredAgencies = (HashSet<FeedScopedId>) preferredAgencies.clone();
            clone.preferredRoutes = preferredRoutes.clone();
            if (this.bikeWalkingOptions != this)
                clone.bikeWalkingOptions = this.bikeWalkingOptions.clone();
            else
                clone.bikeWalkingOptions = clone;
            return clone;
        } catch (CloneNotSupportedException e) {
            /* this will never happen since our super is the cloneable object */
            throw new RuntimeException(e);
        }
    }

    public RoutingRequest reversedClone() {
        RoutingRequest ret = this.clone();
        ret.setArriveBy(!ret.arriveBy);
        ret.useBikeRentalAvailabilityInformation = false;
        return ret;
    }

    public void setRoutingContext(Graph graph) {
        if (rctx == null) {
            // graphService.getGraph(routerId)
            this.rctx = new RoutingContext(this, graph);
            // check after back reference is established, to allow temp edge cleanup on exceptions
            this.rctx.checkIfVerticesFound();
        } else {
            if (rctx.graph == graph) {
                LOG.debug("keeping existing routing context");
                return;
            } else {
                LOG.error("attempted to reset routing context using a different graph");
                return;
            }
        }
    }

    /**
     * For use in tests. Force RoutingContext to specific vertices rather than making temp edges.
     * TODO rename - this is not a "setter", it creates a new routingContext, which has side effects on Graph
     *               (Constructors with side effects on their parameters are a bad design).
     */
    public void setRoutingContext(Graph graph, Edge fromBackEdge, Vertex from, Vertex to) {
        // normally you would want to tear down the routing context...
        // but this method is mostly used in tests, and teardown interferes with testHalfEdges
        // FIXME here, or in test, and/or in other places like TSP that use this method
        // if (rctx != null)
        // this.rctx.destroy();
        this.rctx = new RoutingContext(this, graph, from, to);
        this.rctx.originBackEdge = fromBackEdge;
    }

    public void setRoutingContext(Graph graph, Vertex from, Vertex to) {
        setRoutingContext(graph, null, from, to);
    }

    public void setRoutingContext(Graph graph, Set<Vertex> from, Set<Vertex> to) {
        setRoutingContext(graph, null, from, to);
    }

    public void setRoutingContext(Graph graph, Edge fromBackEdge, Set<Vertex> from, Set<Vertex> to) {
        // normally you would want to tear down the routing context...
        // but this method is mostly used in tests, and teardown interferes with testHalfEdges
        // FIXME here, or in test, and/or in other places like TSP that use this method
        // if (rctx != null)
        // this.rctx.destroy();
        this.rctx = new RoutingContext(this, graph, from, to);
        this.rctx.originBackEdge = fromBackEdge;
    }

    /** For use in tests. Force RoutingContext to specific vertices rather than making temp edges. */
    public void setRoutingContext(Graph graph, String from, String to) {
        this.setRoutingContext(graph, graph.getVertex(from), graph.getVertex(to));
    }

    /** Used in internals API. Make a RoutingContext with no origin or destination vertices specified. */
    public void setDummyRoutingContext(Graph graph) {
        this.setRoutingContext(graph, "", "");
    }

    public RoutingContext getRoutingContext() {
        return this.rctx;
    }

    /** Tear down any routing context (remove temporary edges from edge lists) */
    public void cleanup() {
        if (this.rctx == null)
            LOG.warn("routing context was not set, cannot destroy it.");
        else {
            rctx.destroy();
            LOG.debug("routing context destroyed");
        }
    }

    /**
     * @param mode
     * @return The road speed for a specific traverse mode.
     */
    public double getSpeed(TraverseMode mode) {
        switch (mode) {
        case WALK:
            return walkSpeed;
        case BICYCLE:
            return bikeSpeed;
        case CAR:
            return carSpeed;
        default:
            break;
        }
        throw new IllegalArgumentException("getSpeed(): Invalid mode " + mode);
    }

    /** @return The highest speed for all possible road-modes. */
    public double getStreetSpeedUpperBound() {
        // Assume carSpeed > bikeSpeed > walkSpeed
        if (streetSubRequestModes.getCar())
            return carSpeed;
        if (streetSubRequestModes.getBicycle())
            return bikeSpeed;
        return walkSpeed;
    }

    public void setMaxWalkDistance(double maxWalkDistance) {
        if (maxWalkDistance > 0) {
            this.maxWalkDistance = maxWalkDistance;
            bikeWalkingOptions.maxWalkDistance = maxWalkDistance;
        }
    }

    public void setMaxPreTransitTime(int maxPreTransitTime) {
        if (maxPreTransitTime > 0) {
            this.maxPreTransitTime = maxPreTransitTime;
            bikeWalkingOptions.maxPreTransitTime = maxPreTransitTime;
        }
    }

    public void setWalkReluctance(double walkReluctance) {
        if (walkReluctance > 0) {
            this.walkReluctance = walkReluctance;
            // Do not set bikeWalkingOptions.walkReluctance here, because that needs a higher value.
        }
    }

    public void setWaitReluctance(double waitReluctance) {
        if (waitReluctance > 0) {
            this.waitReluctance = waitReluctance;
        }
    }

    public void setWaitAtBeginningFactor(double waitAtBeginningFactor) {
        if (waitAtBeginningFactor > 0) {
            this.waitAtBeginningFactor = waitAtBeginningFactor;
        }
    }

    public Set<FeedScopedId> getBannedRoutes(Collection<Route> routes) {
        Set<FeedScopedId> bannedRoutes = new HashSet<>();
        for (Route route : routes) {
            if (routeIsBanned(route)) {
                bannedRoutes.add(route.getId());
            }
        }
        return bannedRoutes;
    }

    /**
     * Checks if the route is banned. Also, if whitelisting is used, the route (or its agency) has
     * to be whitelisted in order to not count as banned.
     *
     * @param route
     * @return True if the route is banned
     */
    private boolean routeIsBanned(Route route) {
        /* check if agency is banned for this plan */
        if (bannedAgencies != null) {
            if (bannedAgencies.contains(route.getAgency().getId())) {
                return true;
            }
        }

        /* check if route banned for this plan */
        if (bannedRoutes != null) {
            if (bannedRoutes.matches(route)) {
                return true;
            }
        }

        boolean whiteListed = false;
        boolean whiteListInUse = false;

        /* check if agency is whitelisted for this plan */
        if (whiteListedAgencies != null && whiteListedAgencies.size() > 0) {
            whiteListInUse = true;
            if (whiteListedAgencies.contains(route.getAgency().getId())) {
                whiteListed = true;
            }
        }

        /* check if route is whitelisted for this plan */
        if (whiteListedRoutes != null && !whiteListedRoutes.isEmpty()) {
            whiteListInUse = true;
            if (whiteListedRoutes.matches(route)) {
                whiteListed = true;
            }
        }

        if (whiteListInUse && !whiteListed) {
            return true;
        }

        return false;
    }

    /** Check if route is preferred according to this request. */
    public long preferencesPenaltyForRoute(Route route) {
        long preferences_penalty = 0;
        FeedScopedId agencyID = route.getAgency().getId();
        if ((preferredRoutes != null && !preferredRoutes.equals(RouteMatcher.emptyMatcher())) ||
                (preferredAgencies != null && !preferredAgencies.isEmpty())) {
            boolean isPreferedRoute = preferredRoutes != null && preferredRoutes.matches(route);
            boolean isPreferedAgency = preferredAgencies != null && preferredAgencies.contains(agencyID);
            if (!isPreferedRoute && !isPreferedAgency) {
                preferences_penalty += otherThanPreferredRoutesPenalty;
            }
            else {
                preferences_penalty = 0;
            }
        }
        boolean isUnpreferedRoute  = unpreferredRoutes   != null && unpreferredRoutes.matches(route);
        boolean isUnpreferedAgency = unpreferredAgencies != null && unpreferredAgencies.contains(agencyID);
        if (isUnpreferedRoute || isUnpreferedAgency) {
            preferences_penalty += useUnpreferredRoutesPenalty;
        }
        return preferences_penalty;
    }

    /**
     * Sets the bicycle triangle routing parameters -- the relative importance of safety, flatness, and speed.
     * These three fields of the RoutingRequest should have values between 0 and 1, and should add up to 1.
     * This setter function accepts any three numbers and will normalize them to add up to 1.
     */
    public void setTriangleNormalized (double safe, double slope, double time) {
        double total = safe + slope + time;
        safe /= total;
        slope /= total;
        time /= total;
        this.bikeTriangleSafetyFactor = safe;
        this.bikeTriangleSlopeFactor = slope;
        this.bikeTriangleTimeFactor = time;
    }

    public static void assertTriangleParameters(
            Double triangleSafetyFactor,
            Double triangleTimeFactor,
            Double triangleSlopeFactor
    )
            throws ParameterException
    {
        if (triangleSafetyFactor == null && triangleSlopeFactor == null && triangleTimeFactor == null) {
            throw new ParameterException(Message.TRIANGLE_VALUES_NOT_SET);
        }
        if (triangleSafetyFactor == null || triangleSlopeFactor == null || triangleTimeFactor == null) {
            throw new ParameterException(Message.UNDERSPECIFIED_TRIANGLE);
        }
        // FIXME couldn't this be simplified by only specifying TWO of the values?
        if (Math.abs(triangleSafetyFactor + triangleSlopeFactor + triangleTimeFactor - 1) > Math.ulp(1) * 3) {
            throw new ParameterException(Message.TRIANGLE_NOT_AFFINE);
        }
    }

    /** Create a new ShortestPathTree instance using the DominanceFunction specified in this RoutingRequest. */
    public ShortestPathTree getNewShortestPathTree() {
        return this.dominanceFunction.getNewShortestPathTree(this);
    }

    /**
     * Does nothing if different edge is split in origin/destination search
     *
     * But throws TrivialPathException if same edge is split in origin/destination search.
     *
     * used in {@link org.opentripplanner.graph_builder.linking.SimpleStreetSplitter} in {@link org.opentripplanner.graph_builder.linking.SimpleStreetSplitter#link(Vertex, StreetEdge, double, RoutingRequest)}
     * @param edge
     */
    public void canSplitEdge(StreetEdge edge) {
        if (splitEdge == null) {
            splitEdge = edge;
        } else {
            if (splitEdge.equals(edge)) {
                throw new TrivialPathException();
            }
        }

    }

    public Comparator<GraphPath> getPathComparator(boolean compareStartTimes) {
        if ("duration".equals(pathComparator)) {
            return new DurationComparator();
        }
        return new PathComparator(compareStartTimes);
    }
}
