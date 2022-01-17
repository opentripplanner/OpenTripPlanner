package org.opentripplanner.routing.api.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.annotation.Nonnull;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.api.common.LocationStringParser;
import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.common.ParameterException;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimizationParameters;
import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.model.plan.PageCursor;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DurationComparator;
import org.opentripplanner.routing.impl.PathComparator;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.util.time.DateUtils;
import org.opentripplanner.util.time.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class RoutingRequest implements AutoCloseable, Cloneable, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(RoutingRequest.class);

    private static final long NOW_THRESHOLD_SEC = DurationUtils.duration("15h");

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
     * This is the maximum duration in seconds for a direct street search. This is a performance
     * limit and should therefore be set high. Results close to the limit are not guaranteed to be
     * optimal. Use filters to limit what is presented to the client.
     *
     * @see ItineraryListFilter
     */
    public double maxDirectStreetDurationSeconds = Duration.ofHours(4).toSeconds();

    /**
     * This is the maximum duration in seconds for access/egress street searches. This is a
     * performance limit and should therefore be set high. Results close to the limit are not
     * guaranteed to be optimal. Use filters to limit what is presented to the client.
     *
     * @see ItineraryListFilter
     */
    public double maxAccessEgressDurationSeconds = Duration.ofMinutes(45).toSeconds();

    /**
     * Override the settings in maxAccessEgressDurationSeconds for specific street modes. This is
     * done because some street modes searches are much more resource intensive than others.
     */
    public Map<StreetMode, Double> maxAccessEgressDurationSecondsForMode = new HashMap<>();

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
        StreetMode.WALK,
        AllowedTransitMode.getAllTransitModes()
    );

    /**
     * The set of TraverseModes allowed when doing creating sub requests and doing street routing.
     * // TODO OTP2 Street routing requests should eventually be split into its own request class.
     */
    public TraverseModeSet streetSubRequestModes = new TraverseModeSet(TraverseMode.WALK); // defaults in constructor overwrite this

    /**
     * The set of characteristics that the user wants to optimize for -- defaults to SAFE.
     */
    public BicycleOptimizeType bicycleOptimizeType = BicycleOptimizeType.SAFE;

    /**
     * The epoch date/time in seconds that the trip should depart (or arrive, for requests where
     * arriveBy is true)
     */
    private long dateTime = new Date().getTime() / 1000;

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
     * Use {@code null} to unset, and {@link Duration#ZERO} to do one Raptor iteration. The value
     * is dynamically  assigned a suitable value, if not set. In a small to medium size operation
     * you may use a fixed value, like 60 minutes. If you have a mixture of high frequency cities
     * routes and infrequent long distant journeys, the best option is normally to use the dynamic
     * auto assignment.
     * <p>
     * There is no need to set this when going to the next/previous page any more.
     */
    public Duration searchWindow;

    /**
     * Use the cursor to go to the next or previous "page" of trips.
     * You should pass in the original request as is.
     * <p>
     * The next page of itineraries will depart after the current results
     * and the previous page of itineraries will depart before the current results.
     * <p>
     * The paging does not support timeTableView=false and arriveBy=true, this will result in
     * none pareto-optimal results.
     */
    public PageCursor pageCursor;

    /**
     * Search for the best trip options within a time window. If {@code true} two itineraries are
     * considered optimal if one is better on arrival time(earliest wins) and the other is better
     * on departure time(latest wins).
     * <p>
     * In combination with {@code arriveBy} this parameter cover the following 3 use cases:
     * <ul>
     *   <li>
     *     The traveler want to find the best alternative within a time window. Set
     *     {@code timetableView=true} and {@code arriveBy=false}. This is the default, and if the
     *     intention of the traveler is unknown, this gives the best result. This use-case includes
     *     all itineraries in the two next use-cases. This option also work well with paging.
     *
     *     Setting the {@code arriveBy=false}, covers the same use-case, but the input time is
     *     interpreted as latest-arrival-time, and not earliest-departure-time.
     *   </li>
     *   <li>
     *     The traveler want to find the best alternative with departure after a specific time.
     *     For example: I am at the station now and want to get home as quickly as possible.
     *     Set {@code timetableView=true} and {@code arriveBy=false}. Do not support paging.
     *   </li>
     *   <li>
     *     Traveler want to find the best alternative with arrival before specific time. For
     *     example going to a meeting. Set {@code timetableView=true} and {@code arriveBy=false}.
     *     Do not support paging.
     *   </li>
     * </ul>
     * Default: true
     */
    public boolean timetableView = true;

    /**
     * Whether the trip should depart at dateTime (false, the default), or arrive at dateTime.
     */
    public boolean arriveBy = false;

    /**
     * Whether the trip must be wheelchair accessible.
     */
    public boolean wheelchairAccessible = false;

    /**
     * The maximum number of itineraries to return. In OTP1 this parameter terminates the search,
     * but in OTP2 it crops the list of itineraries AFTER the search is complete. This parameter is
     * a post search filter function. A side effect from reducing the result is that OTP2 cannot
     * guarantee to find all pareto-optimal itineraries when paging. Also, a large search-window
     * and a small {@code numItineraries} waste computer CPU calculation time.
     * <p>
     * The default value is 50. This is a reasonably high threshold to prevent large amount of data
     * to be returned. Consider tuning the search-window instead of setting this to a small value.
     */
    public int numItineraries = 50;

    /** The maximum slope of streets for wheelchair trips. */
    public double maxWheelchairSlope = 0.0833333333333; // ADA max wheelchair ramp slope is a good default.

    /** Whether the planner should return intermediate stops lists for transit legs. */
    public boolean showIntermediateStops = false;

    /** max walk/bike speed along streets, in meters per second */
    public double walkSpeed;

    public double bikeSpeed;

    public double bikeWalkingSpeed;

    public double carSpeed;

    public Locale locale = new Locale("en", "US");

    /**
     * An extra penalty added on transfers (i.e. all boardings except the first one).
     * Not to be confused with bikeBoardCost and walkBoardCost, which are the cost of boarding a
     * vehicle with and without a bicycle. The boardCosts are used to model the 'usual' perceived
     * cost of using a transit vehicle, and the transferCost is used when a user requests even
     * less transfers. In the latter case, we don't actually optimize for fewest transfers, as this
     * can lead to absurd results. Consider a trip in New York from Grand Army
     * Plaza (the one in Brooklyn) to Kalustyan's at noon. The true lowest transfers route is to
     * wait until midnight, when the 4 train runs local the whole way. The actual fastest route is
     * the 2/3 to the 4/5 at Nevins to the 6 at Union Square, which takes half an hour.
     * Even someone optimizing for fewest transfers doesn't want to wait until midnight. Maybe they
     * would be willing to walk to 7th Ave and take the Q to Union Square, then transfer to the 6.
     * If this takes less than optimize_transfer_penalty seconds, then that's what we'll return.
     */
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


    /** Configure the transfer optimization */
    public final TransferOptimizationParameters transferOptimization = new TransferOptimizationRequest();

    /**
     * Transit reluctance per mode. Use this to add a advantage(<1.0) to specific modes, or to add
     * a penalty to other modes (> 1.0). The type used here it the internal model
     * {@link TransitMode} make sure to create a mapping for this before using it on the API.
     * <p>
     * If set, the alight-slack-for-mode override the default value {@code 1.0}.
     * <p>
     * This is a scalar multiplied with the time in second on board the transit vehicle. Default
     * value is not-set(empty map).
     */
    private Map<TransitMode, Double> transitReluctanceForMode = new HashMap<>();

    /** A multiplier for how bad walking is, compared to being in transit for equal lengths of time.
     *  Defaults to 2. Empirically, values between 10 and 20 seem to correspond well to the concept
     *  of not wanting to walk too much without asking for totally ridiculous itineraries, but this
     *  observation should in no way be taken as scientific or definitive. Your mileage may vary.
     */
    public double walkReluctance = 2.0;

    public double bikeWalkingReluctance = 5.0;

    public double bikeReluctance = 2.0;

    public double carReluctance = 2.0;

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

    /** Time to rent a vehicle */
    public int vehicleRentalPickupTime = 60;

    /**
     * Cost of renting a vehicle. The cost is a bit more than actual time to model the associated cost and trouble.
     */
    public int vehicleRentalPickupCost = 120;

    /** Time to drop-off a rented vehicle */
    public int vehicleRentalDropoffTime = 30;

    /** Cost of dropping-off a rented vehicle */
    public int vehicleRentalDropoffCost = 30;

    /** The vehicle rental networks which may be used. If empty all networks may be used. */
    public Set<String> allowedVehicleRentalNetworks = Set.of();

    /** The vehicle rental networks which may not be used. If empty, no networks are banned. */
    public Set<String> bannedVehicleRentalNetworks = Set.of();

    /** Time to park a bike */
    public int bikeParkTime = 60;

    /** Cost of parking a bike. */
    public int bikeParkCost = 120;

    /** Time to park a car */
    public int carParkTime = 60;

    /** Cost of parking a car. */
    public int carParkCost = 120;

    /** Tags which are required to use a vehicle parking. If empty, no tags are required. */
    public Set<String> requiredVehicleParkingTags = Set.of();

    /** Tags with which a vehicle parking will not be used. If empty, no tags are banned. */
    public Set<String> bannedVehicleParkingTags = Set.of();

    /**
     * Time to park a car in a park and ride, w/o taking into account driving and walking cost
     * (time to park, switch off, pick your stuff, lock the car, etc...)
     */
    public int carDropoffTime = 120;

    /** Time of getting in/out of a carPickup (taxi) */
    public int carPickupTime = 60;

    /** Cost of getting in/out of a carPickup (taxi) */
    public int carPickupCost = 120;

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

    /**
     * This prevents unnecessary transfers by adding a cost for boarding a vehicle. This is in
     * addition to the cost of the transfer(walking) and waiting-time. It is also in addition to
     * the {@link #transferCost}.
     */
    public int walkBoardCost = 60 * 10;

    /**
     * Separate cost for boarding a vehicle with a bicycle, which is more difficult than on foot.
     * This is in addition to the cost of the transfer(biking) and waiting-time. It is also in
     * addition to the {@link #transferCost}.
     */
    public int bikeBoardCost = 60 * 10;

    /**
     * Do not use certain named agencies
     */
    private Set<FeedScopedId> bannedAgencies = Set.of();

    /**
     * Only use certain named agencies
     */
    private Set<FeedScopedId> whiteListedAgencies = Set.of();


    /**
     * Set of preferred agencies by user.
     */
    @Deprecated
    private Set<FeedScopedId> preferredAgencies = Set.of();

    /**
     * Set of unpreferred agencies for given user.
     */
    @Deprecated
    private Set<FeedScopedId> unpreferredAgencies = Set.of();

    /**
     * Do not use certain named routes.
     * The paramter format is: feedId_routeId,feedId_routeId,feedId_routeId
     * This parameter format is completely nonstandard and should be revised for the 2.0 API, see issue #1671.
     */
    private RouteMatcher bannedRoutes = RouteMatcher.emptyMatcher();

    /** Only use certain named routes
     */
    private RouteMatcher whiteListedRoutes = RouteMatcher.emptyMatcher();

    /** Set of preferred routes by user.
     *
     * @deprecated TODO OTP2 Needs to be implemented
     */
    @Deprecated
    private RouteMatcher preferredRoutes = RouteMatcher.emptyMatcher();

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
    private RouteMatcher unpreferredRoutes = RouteMatcher.emptyMatcher();

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
    public Map<TransitMode, Integer> boardSlackForMode = new EnumMap<>(TransitMode.class);

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
    public Map<TransitMode, Integer> alightSlackForMode = new EnumMap<>(TransitMode.class);

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
    public Integer maxTransfers = 12;

    /**
     * For the bike triangle, how important time is.
     * triangleTimeFactor+triangleSlopeFactor+triangleSafetyFactor == 1
     */
    public double bikeTriangleTimeFactor;

    /** For the bike triangle, how important slope is */
    public double bikeTriangleSlopeFactor;

    /** For the bike triangle, how important safety is */
    public double bikeTriangleSafetyFactor;

    /**
     * Whether or not vehicle rental availability information will be used to plan vehicle rental trips
     */
    public boolean useVehicleRentalAvailabilityInformation = false;

    /**
     * Whether arriving at the destination with a rented (station) bicycle is allowed without
     * dropping it off.
     *
     * @see RoutingRequest#keepingRentedVehicleAtDestinationCost
     * @see VehicleRentalStation#isKeepingVehicleRentalAtDestinationAllowed
     */
    public boolean allowKeepingRentedVehicleAtDestination = false;

    /**
     * The cost of arriving at the destination with the rented bicycle, to discourage doing so.
     *
     * @see RoutingRequest#allowKeepingRentedVehicleAtDestination
     */
    public double keepingRentedVehicleAtDestinationCost = 0;

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
     * When true, trips cancelled in scheduled data are included in this search.
     */
    public boolean includePlannedCancellations = false;

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

    /*
      Additional flags affecting mode transitions.
      This is a temporary solution, as it only covers parking and rental at the beginning of the trip.
    */
    public boolean vehicleRental = false;
    public boolean parkAndRide  = false;
    public boolean carPickup = false;

    public Set<FormFactor> allowedRentalFormFactors = new HashSet<>();

    /**
     * If true vehicle parking availability information will be used to plan park and ride trips where it exists.
     */
    public boolean useVehicleParkingAvailabilityInformation = false;

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
    @Deprecated
    public boolean disableAlertFiltering = false;

    /** Whether to apply the ellipsoid→geoid offset to all elevations in the response */
    public boolean geoidElevation = false;

    /** Which path comparator to use
     *
     * @deprecated TODO OTP2 Regression. Not currently working in OTP2 at the moment.
     */
    @Deprecated
    public String pathComparator = null;


    @Nonnull
    public ItineraryFilterParameters itineraryFilters = ItineraryFilterParameters.createDefault();

    /**
     * The numbers of days before the search date to consider when filtering trips for this search.
     * This is set to 1 to account for trips starting yesterday and crossing midnight so that they
     * can be boarded today. If there are trips that last multiple days, this will need to be
     * increased.
     */
    public int additionalSearchDaysBeforeToday = 1;

    /**
     * The number of days after the search date to consider when filtering trips for this search.
     * This is set to 1 to account for searches today having a search window that crosses midnight
     * and would also need to board trips starting tomorrow. If a search window that lasts more than
     * a day is used, this will need to be increased.
     */
    public int additionalSearchDaysAfterToday = 2;


    /**
     * The filled request parameters for penalties and thresholds values
     */
    public DataOverlayParameters dataOverlay = null;


    /* CONSTRUCTORS */

    /** Constructor for options; modes defaults to walk and transit */
    public RoutingRequest() {
        // http://en.wikipedia.org/wiki/Walking
        walkSpeed = 1.33; // 1.33 m/s ~ 3mph, avg. human speed
        bikeSpeed = 5; // 5 m/s, ~11 mph, a random bicycling speed
        bikeWalkingSpeed = 1.33; // 1.33 m/s ~ 3mph, avg. human speed
        // http://en.wikipedia.org/wiki/Speed_limit
        carSpeed = 40; // 40 m/s, 144 km/h, above the maximum (finite) driving speed limit worldwide
        // Default to walk for access/egress/direct modes and all transit modes

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

    public RoutingRequest(TraverseMode mode, BicycleOptimizeType bicycleOptimizeType) {
        this(new TraverseModeSet(mode), bicycleOptimizeType);
    }

    public RoutingRequest(TraverseModeSet modeSet, BicycleOptimizeType bicycleOptimizeType) {
        this();
        this.bicycleOptimizeType = bicycleOptimizeType;
        this.setStreetSubRequestModes(modeSet);
    }

    public RoutingRequest(RequestModes modes) {
        this();
        this.modes = modes;
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
    }

    public void setMode(TraverseMode mode) {
        setStreetSubRequestModes(new TraverseModeSet(mode));
    }

    public void setStreetSubRequestModes(TraverseModeSet streetSubRequestModes) {
        this.streetSubRequestModes = streetSubRequestModes;
    }

    public void setBicycleOptimizeType(BicycleOptimizeType bicycleOptimizeType) {
        this.bicycleOptimizeType = bicycleOptimizeType;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public void setTransitReluctanceForMode(Map<TransitMode, Double> reluctanceForMode) {
        transitReluctanceForMode.clear();
        transitReluctanceForMode.putAll(reluctanceForMode);
    }

    public Map<TransitMode, Double> transitReluctanceForMode() {
        return Collections.unmodifiableMap(transitReluctanceForMode);
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

    public void setPreferredAgencies(Collection<FeedScopedId> ids) {
        if(ids != null) {
            preferredAgencies = Set.copyOf(ids);
        }
    }

    public void setPreferredAgenciesFromString(String s) {
        if (!s.isEmpty()) {
            preferredAgencies = FeedScopedId.parseListOfIds(s);
        }
    }

    public void setUnpreferredAgencies(Collection<FeedScopedId> ids) {
        if (ids != null) {
            unpreferredAgencies = Set.copyOf(ids);
        }
    }

    public void setUnpreferredAgenciesFromString(String s) {
        if (!s.isEmpty()) {
            unpreferredAgencies = FeedScopedId.parseListOfIds(s);
        }
    }

    public void setBannedAgencies(Collection<FeedScopedId> ids) {
        if (ids != null) {
            bannedAgencies = Set.copyOf(ids);
        }
    }

    public void setBannedAgenciesFromSting(String s) {
        if (!s.isEmpty()) {
            bannedAgencies = FeedScopedId.parseListOfIds(s);
        }
    }

    public void setWhiteListedAgencies(Collection<FeedScopedId> ids) {
        if (ids != null) {
            whiteListedAgencies = Set.copyOf(ids);
        }
    }

    public void setWhiteListedAgenciesFromSting(String s) {
        if (!s.isEmpty()) {
            whiteListedAgencies = FeedScopedId.parseListOfIds(s);
        }
    }

    public void setOtherThanPreferredRoutesPenalty(int penalty) {
        if(penalty < 0) penalty = 0;
        this.otherThanPreferredRoutesPenalty = penalty;
    }

    public void setPreferredRoutes(List<FeedScopedId> routeIds) {
        preferredRoutes = RouteMatcher.idMatcher(routeIds);
    }

    public void setPreferredRoutesFromSting(String s) {
        if (!s.isEmpty()) {
            preferredRoutes = RouteMatcher.parse(s);
        }
        else {
            preferredRoutes = RouteMatcher.emptyMatcher();
        }
    }

    public void setUnpreferredRoutes(List<FeedScopedId> routeIds) {
        unpreferredRoutes = RouteMatcher.idMatcher(routeIds);
    }

    public void setUnpreferredRoutesFromSting(String s) {
        if (!s.isEmpty()) {
            unpreferredRoutes = RouteMatcher.parse(s);
        }
        else {
            unpreferredRoutes = RouteMatcher.emptyMatcher();
        }
    }

    public void setBannedRoutes(List<FeedScopedId> routeIds) {
        bannedRoutes = RouteMatcher.idMatcher(routeIds);
    }

    public void setBannedRoutesFromSting(String s) {
        if (!s.isEmpty()) {
            bannedRoutes = RouteMatcher.parse(s);
        }
        else {
            bannedRoutes = RouteMatcher.emptyMatcher();
        }
    }

    public void setWhiteListedRoutesFromSting(String s) {
        if (!s.isEmpty()) {
            whiteListedRoutes = RouteMatcher.parse(s);
        }
        else {
            whiteListedRoutes = RouteMatcher.emptyMatcher();
        }
    }

    public void setWhiteListedRoutes(List<FeedScopedId> routeIds) {
        whiteListedRoutes = RouteMatcher.idMatcher(routeIds);
    }


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

    /**
     * When a client perform the first search it supply a search time - its is that
     * time. The client may go to the next page, but the original datetime stay unchanged.
     */
    public Instant getDateTimeOriginalSearch() {
        return Instant.ofEpochSecond(dateTime);
    }

    /**
     * The search time for the current page. If the client have moved to the next page
     * then this is the adjusted search time. The search time is adjusted with according to
     * the time-window used.
     */
    public Instant getDateTimeCurrentPage() {
        return pageCursor == null ? Instant.ofEpochSecond(dateTime) : (
                arriveBy
                        ? pageCursor.latestArrivalTime
                        : pageCursor.earliestDepartureTime
        );
    }

    public void setDateTime(Instant dateTime) {
        this.dateTime = dateTime.getEpochSecond();
    }

    public void setDateTime(String date, String time, TimeZone tz) {
        Date dateObject = DateUtils.toDate(date, time, tz);
        setDateTime(dateObject == null ? Instant.now() : dateObject.toInstant());
    }

    /**
     * Is the trip originally planned withing the previous/next 15h?
     */
    public boolean isTripPlannedForNow() {
        return Math.abs(Instant.now().getEpochSecond() - dateTime) < NOW_THRESHOLD_SEC;
    }

    /**
     * Currently only one itinerary is returned for a direct street search
     */
    public int getNumItineraries() {
        return 1;
    }

    public void setPageCursor(String pageCursor) {
        this.pageCursor = PageCursor.decode(pageCursor);
    }

    public void setNumItineraries(int numItineraries) {
        this.numItineraries = numItineraries;
    }

    public String toString() {
        return toString(" ");
    }

    public String toString(String sep) {
        return from + sep + to + sep + getDateTimeOriginalSearch() + sep
                + arriveBy + sep + bicycleOptimizeType + sep + streetSubRequestModes.getAsStr() + sep
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

    /* INSTANCE METHODS */

    public RoutingRequest getStreetSearchRequest(StreetMode streetMode) {
        RoutingRequest streetRequest = this.clone();
        streetRequest.streetSubRequestModes = new TraverseModeSet();

        if (streetMode != null) {
            switch (streetMode) {
                case WALK:
                case FLEXIBLE:
                    streetRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.WALK));
                    break;
                case BIKE:
                    streetRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.BICYCLE));
                    break;
                case BIKE_TO_PARK:
                    streetRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK));
                    streetRequest.parkAndRide = true;
                    break;
                case BIKE_RENTAL:
                    streetRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK));
                    streetRequest.vehicleRental = true;
                    streetRequest.allowedRentalFormFactors.add(FormFactor.BICYCLE);
                    break;
                case SCOOTER_RENTAL:
                    streetRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK));
                    streetRequest.vehicleRental = true;
                    streetRequest.allowedRentalFormFactors.add(FormFactor.SCOOTER);
                    break;
                case CAR:
                    streetRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.CAR));
                    break;
                case CAR_TO_PARK:
                    streetRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK));
                    streetRequest.parkAndRide = true;
                    break;
                case CAR_PICKUP:
                    streetRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK));
                    streetRequest.carPickup = true;
                    break;
                case CAR_RENTAL:
                    streetRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK));
                    streetRequest.vehicleRental = true;
                    streetRequest.allowedRentalFormFactors.add(FormFactor.CAR);
            }
        }

        streetRequest.resetRoutingContext();

        return streetRequest;
    }

    // TODO OTP2 This is needed in order to find the correct from/to vertices for the mode
    private void resetRoutingContext() {
        if (rctx != null) {
            Graph graph = rctx.graph;
            rctx = null;
            setRoutingContext(graph);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public RoutingRequest clone() {
        try {
            RoutingRequest clone = (RoutingRequest) super.clone();
            clone.streetSubRequestModes = streetSubRequestModes.clone();

            clone.allowedVehicleRentalNetworks = Set.copyOf(allowedVehicleRentalNetworks);
            clone.bannedVehicleRentalNetworks = Set.copyOf(bannedVehicleRentalNetworks);

            clone.requiredVehicleParkingTags = Set.copyOf(requiredVehicleParkingTags);
            clone.bannedVehicleParkingTags = Set.copyOf(bannedVehicleParkingTags);

            clone.preferredAgencies = Set.copyOf(preferredAgencies);
            clone.unpreferredAgencies = Set.copyOf(unpreferredAgencies);
            clone.whiteListedAgencies = Set.copyOf(whiteListedAgencies);
            clone.bannedAgencies = Set.copyOf(bannedAgencies);

            clone.bannedRoutes = bannedRoutes.clone();
            clone.whiteListedRoutes = whiteListedRoutes.clone();
            clone.preferredRoutes = preferredRoutes.clone();
            clone.unpreferredRoutes = unpreferredRoutes.clone();

            clone.bannedTrips = (HashMap<FeedScopedId, BannedStopSet>) bannedTrips.clone();

            clone.allowedRentalFormFactors = new HashSet<>(allowedRentalFormFactors);

            return clone;
        } catch (CloneNotSupportedException e) {
            /* this will never happen since our super is the cloneable object */
            throw new RuntimeException(e);
        }
    }

    public RoutingRequest reversedClone() {
        RoutingRequest ret = this.clone();
        ret.setArriveBy(!ret.arriveBy);
        ret.useVehicleRentalAvailabilityInformation = false;
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
        if (rctx != null) {
            this.rctx.destroy();
        }
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
        if (rctx != null)
            this.rctx.destroy();
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
        if (this.rctx != null) {
            try {
                rctx.destroy();
            }
            catch (Exception e) {
                LOG.error("Could not destroy the routing context", e);
            }
        }
    }

    @Override
    public void close() {
        cleanup();
    }

    /**
     * The road speed for a specific traverse mode.
     */
    public double getReluctance(TraverseMode mode, boolean walkingBike) {
        switch (mode) {
            case WALK:
                return walkingBike ? bikeWalkingReluctance : walkReluctance;
            case BICYCLE:
                return bikeReluctance;
            case CAR:
                return carReluctance;
            default:
                throw new IllegalArgumentException("getReluctance(): Invalid mode " + mode);
        }
    }

    /**
     * The road speed for a specific traverse mode.
     */
    public double getSpeed(TraverseMode mode, boolean walkingBike) {
        switch (mode) {
        case WALK:
            return walkingBike ? bikeWalkingSpeed : walkSpeed;
        case BICYCLE:
            return bikeSpeed;
        case CAR:
            return carSpeed;
        default:
            throw new IllegalArgumentException("getSpeed(): Invalid mode " + mode);
        }
    }

    /** @return The highest speed for all possible road-modes. */
    public double getStreetSpeedUpperBound() {
        // Assume carSpeed > bikeSpeed > walkSpeed
        if (streetSubRequestModes.getCar()) {
            return carSpeed;
        }
        if (streetSubRequestModes.getBicycle()) {
            return bikeSpeed;
        }
        return walkSpeed;
    }

    public void setBikeReluctance(double bikeReluctance) {
        if (bikeReluctance > 0) {
            this.bikeReluctance = bikeReluctance;
        }
    }

    public void setBikeWalkingReluctance(double bikeWalkingReluctance) {
        if (bikeWalkingReluctance > 0) {
            this.bikeWalkingReluctance = bikeWalkingReluctance;
        }
    }

    public void setCarReluctance(double carReluctance) {
        if (carReluctance > 0) {
            this.carReluctance = carReluctance;
        }
    }

    public void setWalkReluctance(double walkReluctance) {
        if (walkReluctance > 0) {
            this.walkReluctance = walkReluctance;
        }
    }

    public void setNonTransitReluctance(double nonTransitReluctance) {
        if (nonTransitReluctance > 0) {
            this.bikeReluctance = nonTransitReluctance;
            this.walkReluctance = nonTransitReluctance;
            this.carReluctance = nonTransitReluctance;
            this.bikeWalkingReluctance = nonTransitReluctance * 2.7;
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
        if (bannedRoutes.isEmpty() && bannedAgencies.isEmpty() &&
            whiteListedRoutes.isEmpty() && whiteListedAgencies.isEmpty()
        ) {
            return Set.of();
        }

        Set<FeedScopedId> bannedRoutes = new HashSet<>();
        for (Route route : routes) {
            if (routeIsBanned(route)) {
                bannedRoutes.add(route.getId());
            }
        }
        return bannedRoutes;
    }

    public double getMaxAccessEgressDurationSecondsForMode(StreetMode mode) {
        return maxAccessEgressDurationSecondsForMode.getOrDefault(
            mode,
            maxAccessEgressDurationSeconds
        );
    }

    /**
     * Checks if the route is banned. Also, if whitelisting is used, the route (or its agency) has
     * to be whitelisted in order to not count as banned.
     *
     * @return True if the route is banned
     */
    private boolean routeIsBanned(Route route) {
        /* check if agency is banned for this plan */
        if (!bannedAgencies.isEmpty()) {
            if (bannedAgencies.contains(route.getAgency().getId())) {
                return true;
            }
        }

        /* check if route banned for this plan */
        if (!bannedRoutes.isEmpty()) {
            if (bannedRoutes.matches(route)) {
                return true;
            }
        }

        boolean whiteListed = false;
        boolean whiteListInUse = false;

        /* check if agency is whitelisted for this plan */
        if (!whiteListedAgencies.isEmpty()) {
            whiteListInUse = true;
            if (whiteListedAgencies.contains(route.getAgency().getId())) {
                whiteListed = true;
            }
        }

        /* check if route is whitelisted for this plan */
        if (!whiteListedRoutes.isEmpty()) {
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
        if (!preferredRoutes.equals(RouteMatcher.emptyMatcher()) || !preferredAgencies.isEmpty()) {
            boolean isPreferedRoute = preferredRoutes.matches(route);
            boolean isPreferedAgency = preferredAgencies.contains(agencyID);

            if (!isPreferedRoute && !isPreferedAgency) {
                preferences_penalty += otherThanPreferredRoutesPenalty;
            }
        }
        boolean isUnpreferedRoute  = unpreferredRoutes.matches(route);
        boolean isUnpreferedAgency = unpreferredAgencies.contains(agencyID);
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
    public void setTriangleNormalized(double safe, double slope, double time) {
        if(safe == 0 && slope == 0 && time == 0) {
            var oneThird = 1f /3;
            safe = oneThird;
            slope = oneThird;
            time = oneThird;
        }
        safe = setMinValue(safe);
        slope = setMinValue(slope);
        time = setMinValue(time);

        double total = safe + slope + time;
        if(total != 1) {
            LOG.warn("Bicycle triangle factors don't add up to 1. Values will be scaled proportionally to each other.");
        }

        safe /= total;
        slope /= total;
        time /= total;
        this.bikeTriangleSafetyFactor = safe;
        this.bikeTriangleSlopeFactor = slope;
        this.bikeTriangleTimeFactor = time;
    }

    private double setMinValue(double value) {
        return Math.max(0, value);
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

    public Comparator<GraphPath> getPathComparator(boolean compareStartTimes) {
        if ("duration".equals(pathComparator)) {
            return new DurationComparator();
        }
        return new PathComparator(compareStartTimes);
    }

    /**
     * How close to do you have to be to the start or end to be considered "close".
     *
     * @see RoutingRequest#isCloseToStartOrEnd(Vertex)
     * @see DominanceFunction#betterOrEqualAndComparable(State, State)
     */
    private static final int MAX_CLOSENESS_METERS = 500;
    private Envelope fromEnvelope;
    private Envelope toEnvelope;

    /**
     * Returns if the vertex is considered "close" to the start or end point of the request.
     * This is useful if you want to allow loops in car routes under certain conditions.
     *
     * Note: If you are doing Raptor access/egress searches this method does not take the possible
     * intermediate points (stations) into account. This means that stations might be skipped
     * because a car route to it cannot be found and a suboptimal route to another station is
     * returned instead.
     *
     * If you encounter a case of this, you can adjust this code to take this into account.
     *
     * @see RoutingRequest#MAX_CLOSENESS_METERS
     * @see DominanceFunction#betterOrEqualAndComparable(State, State)
     */
    public boolean isCloseToStartOrEnd(Vertex vertex) {
        if(from == null || to == null || from.getCoordinate() == null || to.getCoordinate() == null) {
            return false;
        }
        if (fromEnvelope == null) {
            fromEnvelope = getEnvelope(from.getCoordinate(), MAX_CLOSENESS_METERS);
        }
        if (toEnvelope == null) {
            toEnvelope = getEnvelope(to.getCoordinate(), MAX_CLOSENESS_METERS);
        }
        return fromEnvelope.intersects(vertex.getCoordinate()) || toEnvelope.intersects(
                vertex.getCoordinate());
    }

    private static Envelope getEnvelope(Coordinate c, int meters) {
        double lat = SphericalDistanceLibrary.metersToDegrees(meters);
        double lon = SphericalDistanceLibrary.metersToLonDegrees(meters, c.y);

        Envelope env = new Envelope(c);
        env.expandBy(lon, lat);

        if (LOG.isDebugEnabled()) {

            var geom = new GeometryFactory().toGeometry(env);
            var geoJson = new GeometryJSON();

            try {
                var stream = new ByteArrayOutputStream();
                geoJson.write(geom, stream);
                LOG.debug(
                        "Computing {}m envelope around coordinate {}. GeoJSON: {}", meters, c,
                        stream.toString()
                );
            }
            catch (IOException e) {
                LOG.error("Could not build debug GeoJSON", e);
            }
        }

        return env;
    }

    /**
     * This method is needed because we sometimes traverse edges with no graph. It returns a
     * default intersection traversal model if no graph is present.
     */
    public IntersectionTraversalCostModel getIntersectionTraversalCostModel() {
        if (this.rctx != null && this.rctx.graph != null) {
            return this.rctx.graph.getIntersectionTraversalModel();
        } else {
            // This is only to maintain compatibility with existing tests
            return Graph.DEFAULT_INTERSECTION_TRAVERSAL_COST_MODEL;
        }
    }
}
