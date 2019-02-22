package org.opentripplanner.routing.core;

import com.google.common.base.Objects;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.error.TrivialPathException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DurationComparator;
import org.opentripplanner.routing.impl.PathComparator;
import org.opentripplanner.routing.request.BannedStopSet;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
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
import java.util.TimeZone;

/**
 * A trip planning request. Some parameters may not be honored by the trip planner for some or all itineraries.
 * For example, maxWalkDistance may be relaxed if the alternative is to not provide a route.
 *
 * All defaults should be specified here in the RoutingRequest, NOT as annotations on query parameters in web services
 * that create RoutingRequests. This establishes a priority chain for default values:
 * RoutingRequest field initializers, then JSON router config, then query parameters.
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

    /** The router ID -- internal ID to switch between router implementation (or graphs) */
    public String routerId = "";

    /** The start location */
    public GenericLocation from;

    /** The end location */
    public GenericLocation to;

    /** An ordered list of intermediate locations to be visited. */
    public List<GenericLocation> intermediatePlaces;

    /**
     * The maximum distance (in meters) the user is willing to walk for access/egress legs.
     * Defaults to unlimited.
     */
    public double maxWalkDistance = Double.MAX_VALUE;

    /**
     * The maximum distance (in meters) the user is willing to walk for transfer legs.
     * Defaults to unlimited. Currently set to be the same value as maxWalkDistance.
     */
    public double maxTransferWalkDistance = Double.MAX_VALUE;

    /**
     * The maximum time (in seconds) of pre-transit travel when using drive-to-transit (park and
     * ride or kiss and ride). By default limited to 30 minutes driving, because if it's unlimited on
     * large graphs the search becomes very slow.
     */
    public int maxPreTransitTime = 30 * 60;

    /** The worst possible time (latest for depart-by and earliest for arrive-by) to accept */
    public long worstTime = Long.MAX_VALUE;

    /** The worst possible weight that we will accept when planning a trip. */
    public double maxWeight = Double.MAX_VALUE;

    /** The maximum duration of a returned itinerary, in hours. */
    public double maxHours = Double.MAX_VALUE;

    /** Whether maxHours limit should consider wait/idle time between the itinerary and the requested arrive/depart time. */
    public boolean useRequestedDateTimeInMaxHours = false;

    /** The set of TraverseModes that a user is willing to use. Defaults to WALK | TRANSIT. */
    public TraverseModeSet modes = new TraverseModeSet("TRANSIT,WALK"); // defaults in constructor overwrite this

    /** The set of characteristics that the user wants to optimize for -- defaults to QUICK, or optimize for transit time. */
    public OptimizeType optimize = OptimizeType.QUICK;
    // TODO this should be completely removed and done only with individual cost parameters
    // Also: apparently OptimizeType only affects BICYCLE mode traversal of street segments.
    // If this is the case it should be very well documented and carried over into the Enum name.

    /** The epoch date/time that the trip should depart (or arrive, for requests where arriveBy is true) */
    public long dateTime = new Date().getTime() / 1000;

    /** Whether the trip should depart at dateTime (false, the default), or arrive at dateTime. */
    public boolean arriveBy = false;

    /** Whether the trip must be wheelchair accessible. */
    public boolean wheelchairAccessible = false;

    /** The maximum number of itineraries to return. */
    public int numItineraries = 3;

    /** The maximum slope of streets for wheelchair trips. */
    public double maxSlope = 0.0833333333333; // ADA max wheelchair ramp slope is a good default.

    /** Whether the planner should return intermediate stops lists for transit legs. */
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
     */
    public int transferPenalty = 0;

    /** A multiplier for how bad walking is, compared to being in transit for equal lengths of time.
     *  Defaults to 2. Empirically, values between 10 and 20 seem to correspond well to the concept
     *  of not wanting to walk too much without asking for totally ridiculous itineraries, but this
     *  observation should in no way be taken as scientific or definitive. Your mileage may vary.*/
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

    /** How much less bad is waiting at the beginning of the trip (replaces waitReluctance on the first boarding) */
    public double waitAtBeginningFactor = 0.4;

    /** This prevents unnecessary transfers by adding a cost for boarding a vehicle. */
    public int walkBoardCost = 60 * 10;

    /** Separate cost for boarding a vehicle with a bicycle, which is more difficult than on foot. */
    public int bikeBoardCost = 60 * 10;

    /**
     * Do not use certain named routes.
     * The paramter format is: feedId_routeId,feedId_routeId,feedId_routeId
     * This parameter format is completely nonstandard and should be revised for the 2.0 API, see issue #1671.
     */
    public RouteMatcher bannedRoutes = RouteMatcher.emptyMatcher();

    /** Only use certain named routes */
    public RouteMatcher whiteListedRoutes = RouteMatcher.emptyMatcher();

    /** Do not use certain named agencies */
    public HashSet<String> bannedAgencies = new HashSet<String>();

    /** Only use certain named agencies */
    public HashSet<String> whiteListedAgencies = new HashSet<String>();

    /** Do not use certain trips */
    public HashMap<FeedScopedId, BannedStopSet> bannedTrips = new HashMap<FeedScopedId, BannedStopSet>();

    /** Do not use certain stops. See for more information the bannedStops property in the RoutingResource class. */
    public StopMatcher bannedStops = StopMatcher.emptyMatcher(); 
    
    /** Do not use certain stops. See for more information the bannedStopsHard property in the RoutingResource class. */
    public StopMatcher bannedStopsHard = StopMatcher.emptyMatcher();
    
    /** Set of preferred routes by user. */
    public RouteMatcher preferredRoutes = RouteMatcher.emptyMatcher();
    
    /** Set of preferred agencies by user. */
    public HashSet<String> preferredAgencies = new HashSet<String>();

    /**
     * Penalty added for using every route that is not preferred if user set any route as preferred. We return number of seconds that we are willing
     * to wait for preferred route.
     */
    public int otherThanPreferredRoutesPenalty = 300;

    /** Set of unpreferred routes for given user. */
    public RouteMatcher unpreferredRoutes = RouteMatcher.emptyMatcher();
    
    /** Set of unpreferred agencies for given user. */
    public HashSet<String> unpreferredAgencies = new HashSet<String>();

    /**
     * Penalty added for using every unpreferred route. We return number of seconds that we are willing to wait for preferred route.
     */
    public int useUnpreferredRoutesPenalty = 300;

    /**
     * A global minimum transfer time (in seconds) that specifies the minimum amount of time that must pass between exiting one transit vehicle and
     * boarding another. This time is in addition to time it might take to walk between transit stops. This time should also be overridden by specific
     * transfer timing information in transfers.txt
     */
    // initialize to zero so this does not inadvertently affect tests, and let Planner handle defaults
    public int transferSlack = 0;

    /** Invariant: boardSlack + alightSlack <= transferSlack. */
    public int boardSlack = 0;

    public int alightSlack = 0;

    public int maxTransfers = 2;

    /**
     * Extensions to the trip planner will require additional traversal options beyond the default 
     * set. We provide an extension point for adding arbitrary parameters with an 
     * extension-specific key.
     */
    public Map<Object, Object> extensions = new HashMap<Object, Object>();

    /** Penalty for using a non-preferred transfer */
    public int nonpreferredTransferPenalty = 180;

    /**
     * For the bike triangle, how important time is. 
     * triangleTimeFactor+triangleSlopeFactor+triangleSafetyFactor == 1
     */
    public double triangleTimeFactor;

    /** For the bike triangle, how important slope is */
    public double triangleSlopeFactor;

    /** For the bike triangle, how important safety is */
    public double triangleSafetyFactor;

    /** Options specifically for the case that you are walking a bicycle. */
    public RoutingRequest bikeWalkingOptions;

    /** This is true when a GraphPath is being traversed in reverse for optimization purposes. */
    public boolean reverseOptimizing = false;

    /** when true, do not use goal direction or stop at the target, build a full SPT */
    public boolean batch = false;

    /**
     * Whether or not bike rental availability information will be used to plan bike rental trips
     */
    public boolean useBikeRentalAvailabilityInformation = false;

    /**
     * The maximum wait time in seconds the user is willing to delay trip start. Only effective in Analyst.
     */
    public long clampInitialWait = -1;

    /**
     * When true, reverse optimize this search on the fly whenever needed, rather than reverse-optimizing the entire path when it's done.
     */
    public boolean reverseOptimizeOnTheFly = false;

    /**
     * When true, do a full reversed search to compact the legs of the GraphPath.
     */
    public boolean compactLegsByReversedSearch = false;

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
     */
    public boolean disableRemainingWeightHeuristic = false;

    /**
     * Extra penalty added for flag-stop boarding/alighting. This parameter only applies to
     * GTFS-Flex routing, which must be explicitly turned on via the useFlexService parameter
     * in router-config.json.
     *
     * In GTFS-Flex, a flag stop is a point at which a vehicle is boarded or alighted which is not
     * a defined stop, e.g. the bus is flagged down in between stops. This parameter is an
     * additional cost added when a board/alight occurs at a flag stop. Increasing this parameter
     * increases the cost of using a flag stop relative to a regular scheduled stop.
     */
    public int flexFlagStopExtraPenalty = 90;

    /**
     * Extra penalty added for deviated-route boarding/alighting. This parameter only applies to
     * GTFS-Flex routing, which must be explicitly turned on via the useFlexService parameter
     * in router-config.json.
     *
     * In GTFS-Flex, deviated-route service is when a vehicle can deviate a certain distance
     * (or within a certain area) in order to drop off or pick up a passenger. This parameter is an
     * additional cost added when a board/alight occurs before/after a deviation. Increasing this
     * parameter increases the cost of using deviated-route service relative to fixed-route.
     */
    public int flexDeviatedRouteExtraPenalty = 180;

    /**
     * Reluctance for call-n-ride. This parameter only applies to GTFS-Flex routing, which must be
     * explicitly turned on via the useFlexService parameter in router-config.json.
     *
     * Call-and-ride service is when a vehicle picks up and drops off a passenger at their origin
     * and destination, without regard to a fixed route. In the GTFS-Flex data standard, call-and-
     * ride service is defined analogously to deviated-route service, but with more permissive
     * parameters. Depending on the particular origin and destination and the size of the area in
     * which a route can deviate, a single route could be used for both deviated-route and call-
     * and-ride service. This parameter is multiplied with the time on board call-and-ride in order to
     * increase the cost of call-and-ride's use relative to normal transit.
     */
    public double flexCallAndRideReluctance = 2.0;

    /**
     * Total time which can be spent on a call-n-ride leg. This parameter only applies to GTFS-Flex
     * routing, which must be explicitly turned on via the useFlexService parameter in
     * router-config.json.
     *
     * "Trip-banning" as a method of obtaining different itinerary results does not work for call-
     * and-ride service: the same trip can be used in different ways, for example to drop off a
     * passenger at different transfer points. Thus, rather than trip-banning, after each itinerary
     * is found, flexMaxCallAndRideSeconds is reduced in order to obtain different itineraries. The
     * new value of the parameter to calculated according to the following formula:
     * min(duration - options.flexReduceCallAndRideSeconds, duration * flexReduceCallAndRideRatio)
     */
    public int flexMaxCallAndRideSeconds = Integer.MAX_VALUE;

    /**
     * Control the reduction of call-and-ride time. This parameter only applies to GTFS-Flex
     * routing, which must be explicitly turned on via the useFlexService parameter in
     * router-config.json.
     *
     * Seconds to reduce flexMaxCallAndRideSeconds after a complete call-n-ride itinerary. The
     * rationale for this parameter is given in the docs for flexMaxCallAndRideSeconds.
     */
    public int flexReduceCallAndRideSeconds = 15 * 60;

    /**
     * Control the reduction of call-and-ride time. This parameter only applies to GTFS-Flex
     * routing, which must be explicitly turned on via the useFlexService parameter in
     * router-config.json.
     *
     * Percentage to reduce flexMaxCallAndRideSeconds after a complete call-n-ride itinerary. The
     * rationale for this parameter is given in the docs for flexMaxCallAndRideSeconds.
     */
    public double flexReduceCallAndRideRatio = 0.5;

    /**
     * Control the size of flag-stop buffer returned in API response. This parameter only applies
     * to GTFS-Flex routing, which must be explicitly turned on via the useFlexService parameter in
     * router-config.json.
     *
     * This allows the UI to specify the length in meters of a segment around flag stops it wants
     * to display, as an indication to the user that the vehicle may be flagged down anywhere on
     * the segment. The backend will supply such a cropped geometry in its response
     * (`Place.flagStopArea`). The segment will be up to flexFlagStopBufferSize meters ahead or
     * behind the board/alight location. The actual length may be less if the board/alight location
     * is near the beginning or end of a route.
     */
    public double flexFlagStopBufferSize;

    /**
     * Whether to use reservation-based services. This parameter only applies to GTFS-Flex
     * routing, which must be explicitly turned on via the useFlexService parameter in
     * router-config.json.
     *
     * In GTFS-Flex, some trips may be defined as "reservation services," which indicates that
     * they require a reservation in order to be used. Such services will only be used if this
     * parameter is true.
     */
    public boolean flexUseReservationServices = true;

    /**
     * Whether to use eligibility-based services. This parameter only applies to GTFS-Flex
     * routing, which must be explicitly turned on via the useFlexService parameter in
     * router-config.json.
     *
     * In GTFS-Flex, some trips may be defined as "eligibility services," which indicates that
     * they require customers to meet a certain set of requirements in order to be used. Such
     * services will only be used if this parameter is true.
     */
    public boolean flexUseEligibilityServices = true;

    /**
     * Whether to ignore DRT time limits. This parameter only applies to GTFS-Flex routing, which
     * must be explicitly turned on via the useFlexService parameter in router-config.json.
     *
     * In GTFS-Flex, deviated-route and call-and-ride service can define a trip-level parameter
     * `drt_advance_book_min`, which determines how far in advance the flexible segment must be
     * scheduled. If `flexIgnoreDrtAdvanceBookMin = false`, OTP will only provide itineraries which
     * are feasible based on that constraint. For example, if the current time is 1:00pm and a
     * particular service must be scheduled one hour in advance, the earliest time the service
     * is usable is 2:00pm.
     */
    public boolean flexIgnoreDrtAdvanceBookMin = false;

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

    /** A transit stop that this trip must start from */
    public FeedScopedId startingTransitStopId;
    
    /** A trip where this trip must start from (depart-onboard routing) */
    public FeedScopedId startingTransitTripId;

    public boolean walkingBike;

    public boolean softWalkLimiting = true;
    public boolean softPreTransitLimiting = true;

    public double softWalkPenalty = 60.0; // a jump in cost when stepping over the walking limit
    public double softWalkOverageRate = 5.0; // a jump in cost for every meter over the walking limit

    public double preTransitPenalty = 300.0; // a jump in cost when stepping over the pre-transit time limit
    public double preTransitOverageRate = 10.0; // a jump in cost for every second over the pre-transit time limit

    /*
      Additional flags affecting mode transitions.
      This is a temporary solution, as it only covers parking and rental at the beginning of the trip.
    */
    public boolean allowBikeRental = false;
    public boolean bikeParkAndRide = false;
    public boolean parkAndRide  = false;
    public boolean kissAndRide  = false;

    /* Whether we are in "long-distance mode". This is currently a server-wide setting, but it could be made per-request. */
    // TODO remove
    public boolean longDistance = false;

    /** Should traffic congestion be considered when driving? */
    public boolean useTraffic = false;

    /** The function that compares paths converging on the same vertex to decide which ones continue to be explored. */
    public DominanceFunction dominanceFunction = new DominanceFunction.Pareto();

    /** Accept only paths that use transit (no street-only paths). */
    public boolean onlyTransitTrips = false;

    /** Option to disable the default filtering of GTFS-RT alerts by time. */
    public boolean disableAlertFiltering = false;

    /** Whether to apply the ellipsoid->geoid offset to all elevations in the response */
    public boolean geoidElevation = false;

    /**
     * How many extra ServiceDays to look in the future (or back, if arriveBy=true)
     *
     * This parameter allows the configuration of how far, in service days, OTP should look for
     * transit service when evaluating the next departure (or arrival) at a stop. In some cases,
     * for example for services which run weekly or monthly, it may make sense to increase this
     * value. Larger values will increase the search time. This does not affect a case where a
     * trip starts multiple service days in the past (e.g. a multiday ferry trip will not be
     * board-able after the 2nd day in the current implementation).
     */
    public int serviceDayLookout = 1;

    /** Which path comparator to use */
    public String pathComparator = null;

    /**
     * This parameter is used in GTFS-Flex routing. Preliminary searches before the main search
     * need to be able to discover TransitStops in order to create call-and-ride legs which allow
     * transfers to fixed-route services.
     */
    public boolean enterStationsWithCar = false;

    /**
     * Minimum length in meters of partial hop edges. This parameter only applies to GTFS-Flex
     * routing, which must be explicitly turned on via the useFlexService parameter in router-
     * config.json.
     *
     * Flag stop and deviated-route service require creating partial PatternHops from points along
     * the route to a scheduled stop. This parameter provides a minimum length of such partial
     * hops, in order to reduce the amount of hops created when they redundant with regular
     * service.
     */
    public int flexMinPartialHopLength = 400;

    /** Saves split edge which can be split on origin/destination search
     *
     * This is used so that TrivialPathException is thrown if origin and destination search would split the same edge
     */
    private StreetEdge splitEdge = null;

    /**
     * Keep track of epoch time the request was created by OTP. This is currently only used by the
     * GTFS-Flex implementation.
     *
     * In GTFS-Flex, deviated-route and call-and-ride service can define a trip-level parameter
     * `drt_advance_book_min`, which determines how far in advance the flexible segment must be
     * scheduled. If `flexIgnoreDrtAdvanceBookMin = false`, OTP will only provide itineraries which
     * are feasible based on that constraint. For example, if the current time is 1:00pm and a
     * particular service must be scheduled one hour in advance, the earliest time the service
     * is usable is 2:00pm.
     */
    public long clockTimeSec;

    /* CONSTRUCTORS */

    /** Constructor for options; modes defaults to walk and transit */
    public RoutingRequest() {
        // http://en.wikipedia.org/wiki/Walking
        walkSpeed = 1.33; // 1.33 m/s ~ 3mph, avg. human speed
        bikeSpeed = 5; // 5 m/s, ~11 mph, a random bicycling speed
        // http://en.wikipedia.org/wiki/Speed_limit
        carSpeed = 40; // 40 m/s, 144 km/h, above the maximum (finite) driving speed limit worldwide
        setModes(new TraverseModeSet(TraverseMode.WALK, TraverseMode.TRANSIT));
        bikeWalkingOptions = this;

        // So that they are never null.
        from = new GenericLocation();
        to = new GenericLocation();
    }

    public RoutingRequest(TraverseModeSet modes) {
        this();
        this.setModes(modes);
    }

    public RoutingRequest(QualifiedModeSet qmodes) {
        this();
        qmodes.applyToRoutingRequest(this);
    }

    public RoutingRequest(String qmodes) {
        this();
        new QualifiedModeSet(qmodes).applyToRoutingRequest(this);
    }

    public RoutingRequest(TraverseMode mode) {
        this();
        this.setModes(new TraverseModeSet(mode));
    }

    public RoutingRequest(TraverseMode mode, OptimizeType optimize) {
        this(new TraverseModeSet(mode), optimize);
    }

    public RoutingRequest(TraverseModeSet modeSet, OptimizeType optimize) {
        this();
        this.optimize = optimize;
        this.setModes(modeSet);
    }

    /* ACCESSOR/SETTER METHODS */

    public boolean transitAllowed() {
        return modes.isTransit();
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
        setModes(new TraverseModeSet(mode));
    }

    public void setModes(TraverseModeSet modes) {
        this.modes = modes;
        if (modes.getBicycle()) {
            // This alternate routing request is used when we get off a bike to take a shortcut and are
            // walking alongside the bike. FIXME why are we only copying certain fields instead of cloning the request?
            bikeWalkingOptions = new RoutingRequest();
            bikeWalkingOptions.setArriveBy(this.arriveBy);
            bikeWalkingOptions.maxWalkDistance = maxWalkDistance;
            bikeWalkingOptions.maxPreTransitTime = maxPreTransitTime;
            bikeWalkingOptions.walkSpeed = walkSpeed * 0.8; // walking bikes is slow
            bikeWalkingOptions.walkReluctance = walkReluctance * 2.7; // and painful
            bikeWalkingOptions.optimize = optimize;
            bikeWalkingOptions.modes = modes.clone();
            bikeWalkingOptions.modes.setBicycle(false);
            bikeWalkingOptions.modes.setWalk(true);
            bikeWalkingOptions.walkingBike = true;
            bikeWalkingOptions.bikeSwitchTime = bikeSwitchTime;
            bikeWalkingOptions.bikeSwitchCost = bikeSwitchCost;
            bikeWalkingOptions.stairsReluctance = stairsReluctance * 5; // carrying bikes on stairs is awful
        } else if (modes.getCar()) {
            bikeWalkingOptions = new RoutingRequest();
            bikeWalkingOptions.setArriveBy(this.arriveBy);
            bikeWalkingOptions.maxWalkDistance = maxWalkDistance;
            bikeWalkingOptions.maxPreTransitTime = maxPreTransitTime;
            bikeWalkingOptions.modes = modes.clone();
            bikeWalkingOptions.modes.setBicycle(false);
            bikeWalkingOptions.modes.setWalk(true);
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

    /**
     * Add an extension parameter with the specified key. Extensions allow you to add arbitrary traversal options.
     */
    public void putExtension(Object key, Object value) {
        extensions.put(key, value);
    }

    /** Determine if a particular extension parameter is present for the specified key. */
    public boolean containsExtension(Object key) {
        return extensions.containsKey(key);
    }

    /** Get the extension parameter with the specified key. */
    @SuppressWarnings("unchecked")
    public <T> T getExtension(Object key) {
        return (T) extensions.get(key);
    }

    /** Returns the model that computes the cost of intersection traversal. */
    public IntersectionTraversalCostModel getIntersectionTraversalCostModel() {
        return traversalCostModel;
    }
    
    /** @return the (soft) maximum walk distance */
    // If transit is not to be used and this is a point to point search
    // or one with soft walk limiting, disable walk limit.
    public double getMaxWalkDistance() {
        if (modes.isTransit() || (batch && !softWalkLimiting)) {
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
            preferredAgencies = new HashSet<>();
            Collections.addAll(preferredAgencies, s.split(","));
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
            unpreferredAgencies = new HashSet<>();
            Collections.addAll(unpreferredAgencies, s.split(","));
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

    public void setBannedStops(String s) {
        if (!s.isEmpty()) {
            bannedStops = StopMatcher.parse(s);
        }
        else {
            bannedStops = StopMatcher.emptyMatcher();
        }
    }

    public void setBannedStopsHard(String s) {
        if (!s.isEmpty()) {
            bannedStopsHard = StopMatcher.parse(s);
        }
        else {
            bannedStopsHard = StopMatcher.emptyMatcher();
        }
    }

    public void setBannedAgencies(String s) {
        if (!s.isEmpty()) {
            bannedAgencies = new HashSet<>();
            Collections.addAll(bannedAgencies, s.split(","));
        }
    }

    public void setWhiteListedAgencies(String s) {
        if (!s.isEmpty()) {
            whiteListedAgencies = new HashSet<>();
            Collections.addAll(whiteListedAgencies, s.split(","));
        }
    }

    public final static int MIN_SIMILARITY = 1000;

    public void setFromString(String from) {
        this.from = GenericLocation.fromOldStyleString(from);
    }

    public void setToString(String to) {
        this.to = GenericLocation.fromOldStyleString(to);
    }

    /**
     * Clear the allowed modes.
     */
    public void clearModes() {
        modes.clear();
    }

    /**
     * Add a TraverseMode to the set of allowed modes.
     */
    public void addMode(TraverseMode mode) {
        modes.setMode(mode, true);
    }

    /**
     * Add multiple modes to the set of allowed modes.
     */
    public void addMode(List<TraverseMode> mList) {
        for (TraverseMode m : mList) {
            addMode(m);
        }
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
        if (modes.isTransit()) {
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
                + arriveBy + sep + optimize + sep + modes.getAsStr() + sep
                + getNumItineraries();
    }

    public void removeMode(TraverseMode mode) {
        modes.setMode(mode, false);
    }

    /**
     * Sets intermediatePlaces by parsing GenericLocations from a list of string.
     */
    public void setIntermediatePlacesFromStrings(List<String> intermediates) {
        this.intermediatePlaces = new ArrayList<GenericLocation>(intermediates.size());
        for (String place : intermediates) {
            intermediatePlaces.add(GenericLocation.fromOldStyleString(place));
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

    public void setTriangleSafetyFactor(double triangleSafetyFactor) {
        this.triangleSafetyFactor = triangleSafetyFactor;
        bikeWalkingOptions.triangleSafetyFactor = triangleSafetyFactor;
    }

    public void setTriangleSlopeFactor(double triangleSlopeFactor) {
        this.triangleSlopeFactor = triangleSlopeFactor;
        bikeWalkingOptions.triangleSlopeFactor = triangleSlopeFactor;
    }

    public void setTriangleTimeFactor(double triangleTimeFactor) {
        this.triangleTimeFactor = triangleTimeFactor;
        bikeWalkingOptions.triangleTimeFactor = triangleTimeFactor;
    }

    public NamedPlace getFromPlace() {
        return this.from.getNamedPlace();
    }

    public NamedPlace getToPlace() {
        return this.to.getNamedPlace();
    }

    /* INSTANCE METHODS */

    @SuppressWarnings("unchecked")
    @Override
    public RoutingRequest clone() {
        try {
            RoutingRequest clone = (RoutingRequest) super.clone();
            clone.bannedRoutes = bannedRoutes.clone();
            clone.bannedTrips = (HashMap<FeedScopedId, BannedStopSet>) bannedTrips.clone();
            clone.bannedStops = bannedStops.clone();
            clone.bannedStopsHard = bannedStopsHard.clone();
            clone.whiteListedAgencies = (HashSet<String>) whiteListedAgencies.clone();
            clone.whiteListedRoutes = whiteListedRoutes.clone();
            clone.preferredAgencies = (HashSet<String>) preferredAgencies.clone();
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
        ret.reverseOptimizing = !ret.reverseOptimizing; // this is not strictly correct
        ret.useBikeRentalAvailabilityInformation = false;
        return ret;
    }

    // Set routing context with passed-in set of temporary vertices. Needed for intermediate places
    // as a consequence of the check that temporary vertices are request-specific.
    public void setRoutingContext(Graph graph, Collection<Vertex> temporaryVertices) {
        if (rctx == null) {
            // graphService.getGraph(routerId)
            this.rctx = new RoutingContext(this, graph, temporaryVertices);
            // check after back reference is established, to allow temp edge cleanup on exceptions
            this.rctx.check();
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

    public void setRoutingContext(Graph graph) {
        setRoutingContext(graph, null);
    }

    /** For use in tests. Force RoutingContext to specific vertices rather than making temp edges. */
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

    /**
     * Equality does not mean that the fields of the two RoutingRequests are identical, but that they will produce the same SPT. This is particularly
     * important when the batch field is set to 'true'. Does not consider the RoutingContext, to allow SPT caching. Intermediate places are also not
     * included because the TSP solver will factor a single intermediate places routing request into several routing requests without intermediates
     * before searching.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RoutingRequest))
            return false;
        RoutingRequest other = (RoutingRequest) o;
        if (this.batch != other.batch)
            return false;
        boolean endpointsMatch;
        if (this.batch) {
            if (this.arriveBy) {
                endpointsMatch = to.equals(other.to);
            } else {
                endpointsMatch = from.equals(other.from);
            }
        } else {
            endpointsMatch = ((from == null && other.from == null) || from.equals(other.from))
                    && ((to == null && other.to == null) || to.equals(other.to));
        }
        return endpointsMatch
                && dateTime == other.dateTime
                && arriveBy == other.arriveBy
                && numItineraries == other.numItineraries // should only apply in non-batch?
                && walkSpeed == other.walkSpeed
                && bikeSpeed == other.bikeSpeed
                && carSpeed == other.carSpeed
                && maxWeight == other.maxWeight
                && worstTime == other.worstTime
                && maxTransfers == other.maxTransfers
                && modes.equals(other.modes)
                && wheelchairAccessible == other.wheelchairAccessible
                && optimize.equals(other.optimize)
                && maxWalkDistance == other.maxWalkDistance
                && maxTransferWalkDistance == other.maxTransferWalkDistance
                && maxPreTransitTime == other.maxPreTransitTime
                && transferPenalty == other.transferPenalty
                && maxSlope == other.maxSlope
                && walkReluctance == other.walkReluctance
                && waitReluctance == other.waitReluctance
                && waitAtBeginningFactor == other.waitAtBeginningFactor
                && walkBoardCost == other.walkBoardCost
                && bikeBoardCost == other.bikeBoardCost
                && bannedRoutes.equals(other.bannedRoutes)
                && bannedTrips.equals(other.bannedTrips)
                && preferredRoutes.equals(other.preferredRoutes)
                && unpreferredRoutes.equals(other.unpreferredRoutes)
                && transferSlack == other.transferSlack
                && boardSlack == other.boardSlack
                && alightSlack == other.alightSlack
                && nonpreferredTransferPenalty == other.nonpreferredTransferPenalty
                && otherThanPreferredRoutesPenalty == other.otherThanPreferredRoutesPenalty
                && useUnpreferredRoutesPenalty == other.useUnpreferredRoutesPenalty
                && triangleSafetyFactor == other.triangleSafetyFactor
                && triangleSlopeFactor == other.triangleSlopeFactor
                && triangleTimeFactor == other.triangleTimeFactor
                && stairsReluctance == other.stairsReluctance
                && elevatorBoardTime == other.elevatorBoardTime
                && elevatorBoardCost == other.elevatorBoardCost
                && elevatorHopTime == other.elevatorHopTime
                && elevatorHopCost == other.elevatorHopCost
                && bikeSwitchTime == other.bikeSwitchTime
                && bikeSwitchCost == other.bikeSwitchCost
                && bikeRentalPickupTime == other.bikeRentalPickupTime
                && bikeRentalPickupCost == other.bikeRentalPickupCost
                && bikeRentalDropoffTime == other.bikeRentalDropoffTime
                && bikeRentalDropoffCost == other.bikeRentalDropoffCost
                && useBikeRentalAvailabilityInformation == other.useBikeRentalAvailabilityInformation
                && extensions.equals(other.extensions)
                && clampInitialWait == other.clampInitialWait
                && reverseOptimizeOnTheFly == other.reverseOptimizeOnTheFly
                && ignoreRealtimeUpdates == other.ignoreRealtimeUpdates
                && disableRemainingWeightHeuristic == other.disableRemainingWeightHeuristic
                && Objects.equal(startingTransitTripId, other.startingTransitTripId)
                && disableAlertFiltering == other.disableAlertFiltering
                && geoidElevation == other.geoidElevation
                && flexFlagStopExtraPenalty == other.flexFlagStopExtraPenalty
                && flexDeviatedRouteExtraPenalty == other.flexDeviatedRouteExtraPenalty
                && flexCallAndRideReluctance == other.flexCallAndRideReluctance
                && flexReduceCallAndRideSeconds == other.flexReduceCallAndRideSeconds
                && flexReduceCallAndRideRatio == other.flexReduceCallAndRideRatio
                && flexFlagStopBufferSize == other.flexFlagStopBufferSize
                && flexUseReservationServices == other.flexUseReservationServices
                && flexUseEligibilityServices == other.flexUseEligibilityServices
                && flexIgnoreDrtAdvanceBookMin == other.flexIgnoreDrtAdvanceBookMin
                && flexMinPartialHopLength == other.flexMinPartialHopLength
                && clockTimeSec == other.clockTimeSec
                && serviceDayLookout == other.serviceDayLookout;
    }

    /**
     * Equality and hashCode should not consider the routing context, to allow SPT caching.
     * When adding fields to the hash code, pick a random large prime number that's not yet in use.
     */
    @Override
    public int hashCode() {
        int hashCode = new Double(walkSpeed).hashCode() + new Double(bikeSpeed).hashCode()
                + new Double(carSpeed).hashCode() + new Double(maxWeight).hashCode()
                + (int) (worstTime & 0xffffffff) + modes.hashCode()
                + (arriveBy ? 8966786 : 0) + (wheelchairAccessible ? 731980 : 0)
                + optimize.hashCode() + new Double(maxWalkDistance).hashCode()
                + new Double(maxTransferWalkDistance).hashCode()
                + new Double(transferPenalty).hashCode() + new Double(maxSlope).hashCode()
                + new Double(walkReluctance).hashCode() + new Double(waitReluctance).hashCode()
                + new Double(waitAtBeginningFactor).hashCode() * 15485863
                + walkBoardCost + bikeBoardCost + bannedRoutes.hashCode()
                + bannedTrips.hashCode() * 1373 + transferSlack * 20996011
                + (int) nonpreferredTransferPenalty + (int) transferPenalty * 163013803
                + new Double(triangleSafetyFactor).hashCode() * 195233277
                + new Double(triangleSlopeFactor).hashCode() * 136372361
                + new Double(triangleTimeFactor).hashCode() * 790052899
                + new Double(stairsReluctance).hashCode() * 315595321
                + maxPreTransitTime * 63061489
                + new Long(clampInitialWait).hashCode() * 209477
                + new Boolean(reverseOptimizeOnTheFly).hashCode() * 95112799
                + new Boolean(ignoreRealtimeUpdates).hashCode() * 154329
                + new Boolean(disableRemainingWeightHeuristic).hashCode() * 193939
                + new Boolean(useTraffic).hashCode() * 10169
                + Integer.hashCode(flexFlagStopExtraPenalty) * 179424691
                + Integer.hashCode(flexDeviatedRouteExtraPenalty) *  7424299
                + Double.hashCode(flexCallAndRideReluctance) * 86666621
                + Integer.hashCode(flexMaxCallAndRideSeconds) * 9994393
                + Integer.hashCode(flexReduceCallAndRideSeconds) * 92356763
                + Double.hashCode(flexReduceCallAndRideRatio) *  171157957
                + Double.hashCode(flexFlagStopBufferSize) * 803989
                + Boolean.hashCode(flexUseReservationServices) * 92429033
                + Boolean.hashCode(flexUseEligibilityServices) * 7916959
                + Boolean.hashCode(flexIgnoreDrtAdvanceBookMin) * 179992387
                + Integer.hashCode(flexMinPartialHopLength) * 15485863
                + Long.hashCode(clockTimeSec) * 833389
                + new Boolean(disableRemainingWeightHeuristic).hashCode() * 193939
                + new Boolean(useTraffic).hashCode() * 10169
                + Integer.hashCode(serviceDayLookout) * 31558519;

        if (batch) {
            hashCode *= -1;
            // batch mode, only one of two endpoints matters
            if (arriveBy) {
                hashCode += to.hashCode() * 1327144003;
            } else {
                hashCode += from.hashCode() * 524287;
            }
            hashCode += numItineraries; // why is this only present here?
        } else {
            // non-batch, both endpoints matter
            hashCode += from.hashCode() * 524287;
            hashCode += to.hashCode() * 1327144003;
        }
        return hashCode;
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
        if (modes.getCar())
            return carSpeed;
        if (modes.getBicycle())
            return bikeSpeed;
        return walkSpeed;
    }

    /**
     * @param mode
     * @return The board cost for a specific traverse mode.
     */
    public int getBoardCost(TraverseMode mode) {
        if (mode == TraverseMode.BICYCLE)
            return bikeBoardCost;
        // I assume you can't bring your car in the bus
        return walkBoardCost;
    }

    /** @return The lower boarding cost for all possible road-modes. */
    public int getBoardCostLowerBound() {
        // Assume walkBoardCost < bikeBoardCost
        if (modes.getWalk())
            return walkBoardCost;
        return bikeBoardCost;
    }

    /**
     * @return The time it actually takes to board a vehicle. Could be significant eg. on airplanes and ferries
     */
    public int getBoardTime(TraverseMode transitMode) {
        Integer i = this.rctx.graph.boardTimes.get(transitMode);
        return i == null ? 0 : i;
    }

    /**
     * @return The time it actually takes to alight a vehicle. Could be significant eg. on airplanes and ferries
     */
    public int getAlightTime(TraverseMode transitMode) {
        Integer i = this.rctx.graph.alightTimes.get(transitMode);
        return i == null ? 0 : i;
    }

    private String getRouteOrAgencyStr(HashSet<String> strings) {
        StringBuilder builder = new StringBuilder();
        for (String agency : strings) {
            builder.append(agency);
            builder.append(",");
        }
        if (builder.length() > 0) {
            // trim trailing comma
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    public void setMaxWalkDistance(double maxWalkDistance) {
        if (maxWalkDistance >= 0) {
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

    public void banTrip(FeedScopedId trip) {
        bannedTrips.put(trip, BannedStopSet.ALL);
    }

    public boolean routeIsBanned(Route route) {
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
        String agencyID = route.getAgency().getId();
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
     * Get the maximum expected speed over all transit modes.
     * TODO derive actual speeds from GTFS feeds. On the other hand, that's what the bidirectional heuristic does on the fly.
     */
    public double getTransitSpeedUpperBound() {
        if (modes.contains(TraverseMode.RAIL)) {
            return 84; // 300kph typical peak speed of a TGV
        }
        if (modes.contains(TraverseMode.CAR)) {
            return 40; // 130kph max speed of a car on a highway
        }
        // Considering that buses can travel on highways, return the same max speed for all other transit.
        return 40; // TODO find accurate max speeds
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
        this.triangleSafetyFactor = safe;
        this.triangleSlopeFactor = slope;
        this.triangleTimeFactor = time;
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

    public void resetClockTime() {
        this.clockTimeSec = System.currentTimeMillis() / 1000;
    }

    public void setServiceDayLookout(int serviceDayLookout) {
        this.serviceDayLookout = serviceDayLookout;
    }

    public Comparator<GraphPath> getPathComparator(boolean compareStartTimes) {
        if ("duration".equals(pathComparator)) {
            return new DurationComparator();
        }
        return new PathComparator(compareStartTimes);
    }

}
