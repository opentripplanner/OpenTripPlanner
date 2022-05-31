package org.opentripplanner.routing.api.request;

import static org.opentripplanner.util.time.DurationUtils.durationInSeconds;

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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.common.LocationStringParser;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.pagecursor.PageCursor;
import org.opentripplanner.model.plan.pagecursor.PageType;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimizationParameters;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DurationComparator;
import org.opentripplanner.routing.impl.PathComparator;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.util.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A trip planning request. Some parameters may not be honored by the trip planner for some or all
 * itineraries. For example, maxWalkDistance may be relaxed if the alternative is to not provide a
 * route.
 * <p>
 * All defaults should be specified here in the RoutingRequest, NOT as annotations on query
 * parameters in web services that create RoutingRequests. This establishes a priority chain for
 * default values: RoutingRequest field initializers, then JSON router config, then query
 * parameters.
 *
 * @Deprecated tag is added to all parameters that are not currently functional in either the Raptor
 * router or other non-transit routing (walk, bike, car etc.)
 * <p>
 * TODO OTP2 Many fields are deprecated in this class, the reason is documented in the
 *           RoutingResource class, not here. Eventually the field will be removed from this
 *           class, but we want to keep it in the RoutingResource as long as we support the
 *           REST API.
 */
public class RoutingRequest implements Cloneable, Serializable {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = LoggerFactory.getLogger(RoutingRequest.class);

  private static final long NOW_THRESHOLD_SEC = durationInSeconds("15h");

  /* FIELDS UNIQUELY IDENTIFYING AN SPT REQUEST */
  /**
   * How close to do you have to be to the start or end to be considered "close".
   *
   * @see RoutingRequest#isCloseToStartOrEnd(Vertex)
   * @see DominanceFunction#betterOrEqualAndComparable(State, State)
   */
  private static final int MAX_CLOSENESS_METERS = 500;
  /** The complete list of incoming query parameters. */
  public final HashMap<String, String> parameters = new HashMap<>();
  /** Configure the transfer optimization */
  public final TransferOptimizationParameters transferOptimization = new TransferOptimizationRequest();
  /**
   * Transit reluctance per mode. Use this to add a advantage(<1.0) to specific modes, or to add a
   * penalty to other modes (> 1.0). The type used here it the internal model {@link TransitMode}
   * make sure to create a mapping for this before using it on the API.
   * <p>
   * If set, the alight-slack-for-mode override the default value {@code 1.0}.
   * <p>
   * This is a scalar multiplied with the time in second on board the transit vehicle. Default value
   * is not-set(empty map).
   */
  private final Map<TransitMode, Double> transitReluctanceForMode = new HashMap<>();
  /** The start location */
  public GenericLocation from;
  /** The end location */
  public GenericLocation to;

  /**
   * An ordered list of intermediate locations to be visited.
   *
   * @deprecated TODO OTP2 - Regression. Not currently working in OTP2. Must be re-implemented
   * - using raptor.
   */
  @Deprecated
  public List<GenericLocation> intermediatePlaces;

  /**
   * This is the maximum duration for a direct street search. This is a performance limit and should
   * therefore be set high. Results close to the limit are not guaranteed to be optimal.
   * Use filters to limit what is presented to the client.
   *
   * @see ItineraryListFilter
   */
  public Duration maxDirectStreetDuration = Duration.ofHours(4);

  /**
   * Override the settings in maxDirectStreetDuration for specific street modes. This is done
   * because some street modes searches are much more resource intensive than others.
   */
  public Map<StreetMode, Duration> maxDirectStreetDurationForMode = new HashMap<>();

  /**
   * This is the maximum duration for access/egress street searches. This is a performance limit and
   * should therefore be set high. Results close to the limit are not guaranteed to be optimal.
   * Use filters to limit what is presented to the client.
   *
   * @see ItineraryListFilter
   */
  public Duration maxAccessEgressDuration = Duration.ofMinutes(45);
  /**
   * Override the settings in maxAccessEgressDuration for specific street modes. This is done
   * because some street modes searches are much more resource intensive than others.
   */
  public Map<StreetMode, Duration> maxAccessEgressDurationForMode = new HashMap<>();
  /**
   * The access/egress/direct/transit modes allowed for this main request. The parameter
   * "streetSubRequestModes" below is used for a single A Star sub request.
   * <p>
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
   * The set of TraverseModes allowed when doing creating sub requests and doing street routing. //
   * TODO OTP2 Street routing requests should eventually be split into its own request class.
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
  private Instant dateTime = Instant.now();
  /**
   * This is the time/duration in seconds from the earliest-departure-time(EDT) to
   * latest-departure-time(LDT). In case of a reverse search it will be the time from earliest to
   * latest arrival time (LAT - EAT).
   * <p>
   * All optimal travels that depart within the search window is guarantied to be found.
   * <p>
   * This is sometimes referred to as the Range Raptor Search Window - but could be used in a none
   * Transit search as well; Hence this is named search-window and not raptor-search-window. Do not
   * confuse this with the travel-window, which is the time between EDT to LAT.
   * <p>
   * Use {@code null} to unset, and {@link Duration#ZERO} to do one Raptor iteration. The value is
   * dynamically  assigned a suitable value, if not set. In a small to medium size operation you may
   * use a fixed value, like 60 minutes. If you have a mixture of high frequency cities routes and
   * infrequent long distant journeys, the best option is normally to use the dynamic auto
   * assignment.
   * <p>
   * There is no need to set this when going to the next/previous page any more.
   */
  public Duration searchWindow;
  /**
   * The expected maximum time a journey can last across all possible journeys for the current
   * deployment. Normally you would just do an estimate and add enough slack, so you are sure that
   * there is no journeys that falls outside this window. The parameter is used find all possible
   * dates for the journey and then search only the services which run on those dates. The duration
   * must include access, egress, wait-time and transit time for the whole journey. It should also
   * take low frequency days/periods like holidays into account. In other words, pick the two points
   * within your area that has the worst connection and then try to travel on the worst possible
   * day, and find the maximum journey duration. Using a value that is too high has the effect of
   * including more patterns in the search, hence, making it a bit slower. Recommended values would
   * be from 12 hours(small town/city), 1 day (region) to 2 days (country like Norway).
   */
  public Duration maxJourneyDuration = Duration.ofHours(24);
  /**
   * Use the cursor to go to the next or previous "page" of trips. You should pass in the original
   * request as is.
   * <p>
   * The next page of itineraries will depart after the current results and the previous page of
   * itineraries will depart before the current results.
   * <p>
   * The paging does not support timeTableView=false and arriveBy=true, this will result in none
   * pareto-optimal results.
   */
  public PageCursor pageCursor;
  /**
   * Search for the best trip options within a time window. If {@code true} two itineraries are
   * considered optimal if one is better on arrival time(earliest wins) and the other is better on
   * departure time(latest wins).
   * <p>
   * In combination with {@code arriveBy} this parameter cover the following 3 use cases:
   * <ul>
   *   <li>
   *     The traveler want to find the best alternative within a time window. Set
   *     {@code timetableView=true} and {@code arriveBy=false}. This is the default, and if the
   *     intention of the traveler is unknown, this gives the best result. This use-case includes
   *     all itineraries in the two next use-cases. This option also work well with paging.
   * <p>
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
   * Whether the trip must be wheelchair-accessible and how strictly this should be interpreted.
   */
  @Nonnull
  public WheelchairAccessibilityRequest wheelchairAccessibility =
    WheelchairAccessibilityRequest.DEFAULT;

  /**
   * The maximum number of itineraries to return. In OTP1 this parameter terminates the search, but
   * in OTP2 it crops the list of itineraries AFTER the search is complete. This parameter is a post
   * search filter function. A side effect from reducing the result is that OTP2 cannot guarantee to
   * find all pareto-optimal itineraries when paging. Also, a large search-window and a small {@code
   * numItineraries} waste computer CPU calculation time.
   * <p>
   * The default value is 50. This is a reasonably high threshold to prevent large amount of data to
   * be returned. Consider tuning the search-window instead of setting this to a small value.
   */
  public int numItineraries = 50;

  /** Whether the planner should return intermediate stops lists for transit legs. */
  public boolean showIntermediateStops = false;
  /**
   * Human walk speed along streets, in meters per second.
   * <p>
   * Default: 1.33 m/s ~ 3mph, <a href="http://en.wikipedia.org/wiki/Walking">avg. human walk
   * speed</a>
   */
  public double walkSpeed = 1.33;
  /**
   * Default: 5 m/s, ~11 mph, a random bicycling speed
   */
  public double bikeSpeed = 5;
  /**
   * Default: 1.33 m/s ~ Same as walkSpeed
   */
  public double bikeWalkingSpeed = 1.33;
  /**
   * Max car speed along streets, in meters per second.
   * <p>
   * Default: 40 m/s, 144 km/h, above the maximum (finite) driving speed limit worldwide.
   */
  public double carSpeed = 40.0;
  public Locale locale = new Locale("en", "US");
  /**
   * An extra penalty added on transfers (i.e. all boardings except the first one). Not to be
   * confused with bikeBoardCost and walkBoardCost, which are the cost of boarding a vehicle with
   * and without a bicycle. The boardCosts are used to model the 'usual' perceived cost of using a
   * transit vehicle, and the transferCost is used when a user requests even less transfers. In the
   * latter case, we don't actually optimize for fewest transfers, as this can lead to absurd
   * results. Consider a trip in New York from Grand Army Plaza (the one in Brooklyn) to Kalustyan's
   * at noon. The true lowest transfers route is to wait until midnight, when the 4 train runs local
   * the whole way. The actual fastest route is the 2/3 to the 4/5 at Nevins to the 6 at Union
   * Square, which takes half an hour. Even someone optimizing for fewest transfers doesn't want to
   * wait until midnight. Maybe they would be willing to walk to 7th Ave and take the Q to Union
   * Square, then transfer to the 6. If this takes less than optimize_transfer_penalty seconds, then
   * that's what we'll return.
   */
  public int transferCost = 0;

  /**
   * Penalty for using a non-preferred transfer
   *
   * @deprecated TODO OTP2 Regression. Not currently working in OTP2. We might not implement the
   * old functionality the same way, but we will try to map this parameter
   * so it does work similar as before.
   */
  @Deprecated
  public int nonpreferredTransferCost = 180;

  /**
   * A multiplier for how bad walking is, compared to being in transit for equal
   * lengths of time. Empirically, values between 2 and 4 seem to correspond
   * well to the concept of not wanting to walk too much without asking for
   * totally ridiculous itineraries, but this observation should in no way be
   * taken as scientific or definitive. Your mileage may vary. See
   * https://github.com/opentripplanner/OpenTripPlanner/issues/4090 for impact on
   * performance with high values. Default value: 2.0
   */
  public double walkReluctance = 2.0;
  public double bikeWalkingReluctance = 5.0;
  public double bikeReluctance = 2.0;
  public double carReluctance = 2.0;
  /**
   * How much more time does it take to walk a flight of stairs compared to walking a similar
   * horizontal length
   * <p>
   * Default value is based on: Fujiyama, T., & Tyler, N. (2010). Predicting the walking speed of
   * pedestrians on stairs. Transportation Planning and Technology, 33(2), 177–202.
   */
  public double stairsTimeFactor = 3.0;
  /** Used instead of walk reluctance for stairs */
  public double stairsReluctance = 2.0;
  /** Multiplicative factor on expected turning time. */
  public double turnReluctance = 1.0;
  /**
   * How long does it take to get an elevator, on average (actually, it probably should be a bit
   * *more* than average, to prevent optimistic trips)? Setting it to "seems like forever," while
   * accurate, will probably prevent OTP from working correctly.
   */
  // TODO: how long does it /really/ take to get an elevator?
  public int elevatorBoardTime = 90;
  /** What is the cost of boarding an elevator? */
  public int elevatorBoardCost = 90;
  /** How long does it take to advance one floor on an elevator? */
  public int elevatorHopTime = 20;

  // it is assumed that getting off an elevator is completely free
  /** What is the cost of travelling one floor on an elevator? */
  public int elevatorHopCost = 20;
  /** Time to get on and off your own bike */
  public int bikeSwitchTime;
  /** Cost of getting on and off your own bike */
  public int bikeSwitchCost;
  /** Time to rent a vehicle */
  public int vehicleRentalPickupTime = 60;
  /**
   * Cost of renting a vehicle. The cost is a bit more than actual time to model the associated cost
   * and trouble.
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
   * Time to park a car in a park and ride, w/o taking into account driving and walking cost (time
   * to park, switch off, pick your stuff, lock the car, etc...)
   */
  public int carDropoffTime = 120;
  /** Time of getting in/out of a carPickup (taxi) */
  public int carPickupTime = 60;
  /** Cost of getting in/out of a carPickup (taxi) */
  public int carPickupCost = 120;
  /**
   * How much worse is waiting for a transit vehicle than being on a transit vehicle, as a
   * multiplier. The default value treats wait and on-vehicle time as the same.
   * <p>
   * It may be tempting to set this higher than walkReluctance (as studies often find this kind of
   * preferences among riders) but the planner will take this literally and walk down a transit line
   * to avoid waiting at a stop. This used to be set less than 1 (0.95) which would make waiting
   * offboard preferable to waiting onboard in an interlined trip. That is also undesirable.
   * <p>
   * If we only tried the shortest possible transfer at each stop to neighboring stop patterns, this
   * problem could disappear.
   */
  public double waitReluctance = 1.0;

  /**
   * How much less bad is waiting at the beginning of the trip (replaces waitReluctance on the first
   * boarding)
   *
   * @deprecated TODO OTP2 Probably a regression, but I'm not sure it worked correctly in OTP 1.X
   * either. It could be a part of itinerary-filtering after a Raptor search.
   */
  @Deprecated
  public double waitAtBeginningFactor = 0.4;

  /**
   * This prevents unnecessary transfers by adding a cost for boarding a vehicle. This is in
   * addition to the cost of the transfer(walking) and waiting-time. It is also in addition to the
   * {@link #transferCost}.
   */
  public int walkBoardCost = 60 * 10;
  /**
   * Separate cost for boarding a vehicle with a bicycle, which is more difficult than on foot. This
   * is in addition to the cost of the transfer(biking) and waiting-time. It is also in addition to
   * the {@link #transferCost}.
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
   * Do not use certain named routes. The paramter format is: feedId_routeId,feedId_routeId,feedId_routeId
   * This parameter format is completely nonstandard and should be revised for the 2.0 API, see
   * issue #1671.
   */
  private RouteMatcher bannedRoutes = RouteMatcher.emptyMatcher();
  /**
   * Only use certain named routes
   */
  private RouteMatcher whiteListedRoutes = RouteMatcher.emptyMatcher();

  /**
   * Set of preferred routes by user.
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
   */
  public Set<FeedScopedId> bannedTrips = Set.of();
  /**
   * A global minimum transfer time (in seconds) that specifies the minimum amount of time that must
   * pass between exiting one transit vehicle and boarding another. This time is in addition to time
   * it might take to walk between transit stops, the {@link #alightSlack}, and the {@link
   * #boardSlack}. This time should also be overridden by specific transfer timing information in
   * transfers.txt
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
   * Has information how much time boarding a vehicle takes. Can be significant eg in airplanes or
   * ferries.
   * <p>
   * If set, the board-slack-for-mode override the more general {@link #boardSlack}. This enables
   * configuring the board-slack for airplane boarding to be 30 minutes and a slack for bus of 2
   * minutes.
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
   * Has information how much time alighting a vehicle takes. Can be significant eg in airplanes or
   * ferries.
   * <p>
   * If set, the alight-slack-for-mode override the more general {@link #alightSlack}. This enables
   * configuring the alight-slack for train alighting to be 4 minutes and a bus alight slack to be 0
   * minutes.
   * <p>
   * Unit is seconds. Default value is not-set(empty map).
   */
  public Map<TransitMode, Integer> alightSlackForMode = new EnumMap<>(TransitMode.class);
  /**
   * Ideally maxTransfers should be set in the router config, not here. Instead the client should be
   * able to pass in a parameter for the max number of additional/extra transfers relative to the
   * best trip (with the fewest possible transfers) within constraint of the other search
   * parameters(TODO OTP2 Expose {@link org.opentripplanner.transit.raptor.api.request.SearchParams#numberOfAdditionalTransfers()}
   * in APIs). This might be to complicated to explain to the customer, so we might stick to the old
   * limit, but that have side-effects that you might not find any trips on a day where a critical
   * part of the trip is not available, because of some real-time disruption.
   * <p>
   * See https://github.com/opentripplanner/OpenTripPlanner/issues/2886
   */
  public Integer maxTransfers = 12;
  /**
   * For the bike triangle, how important time is. triangleTimeFactor+triangleSlopeFactor+triangleSafetyFactor
   * == 1
   */
  public double bikeTriangleTimeFactor;
  /** For the bike triangle, how important slope is */
  public double bikeTriangleSlopeFactor;
  /** For the bike triangle, how important safety is */
  public double bikeTriangleSafetyFactor;
  /**
   * Whether or not vehicle rental availability information will be used to plan vehicle rental
   * trips
   */
  public boolean useVehicleRentalAvailabilityInformation = false;
  /**
   * Whether arriving at the destination with a rented (station) bicycle is allowed without dropping
   * it off.
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
   * A transit stop that this trip must start from
   *
   * @deprecated TODO OTP2 Is this in use, what is is used for. It seems to overlap with
   * the fromPlace parameter. Is is used for onBoard routing only?
   */
  @Deprecated
  public FeedScopedId startingTransitStopId;

  /**
   * A trip where this trip must start from (depart-onboard routing)
   *
   * @deprecated TODO OTP2 Regression. Not currently working in OTP2. We might not implement the
   * old functionality the same way, but we will try to map this parameter
   * so it does work similar as before.
   */
  @Deprecated
  public FeedScopedId startingTransitTripId;

  /*
      Additional flags affecting mode transitions.
      This is a temporary solution, as it only covers parking and rental at the beginning of the trip.
    */
  public boolean vehicleRental = false;
  public boolean parkAndRide = false;
  public boolean carPickup = false;
  public Set<FormFactor> allowedRentalFormFactors = new HashSet<>();
  /**
   * If true vehicle parking availability information will be used to plan park and ride trips where
   * it exists.
   */
  public boolean useVehicleParkingAvailabilityInformation = false;

  /**
   * Accept only paths that use transit (no street-only paths).
   *
   * @Deprecated TODO OTP2 Regression. Not currently working in OTP2. This is only used in the
   * deprecated Transmodel GraphQL API.
   */
  @Deprecated
  public boolean onlyTransitTrips = false;

  /** Option to disable the default filtering of GTFS-RT alerts by time. */
  @Deprecated
  public boolean disableAlertFiltering = false;

  /** Whether to apply the ellipsoid→geoid offset to all elevations in the response */
  public boolean geoidElevation = false;

  /**
   * Which path comparator to use
   *
   * @deprecated TODO OTP2 Regression. Not currently working in OTP2 at the moment.
   */
  @Deprecated
  public String pathComparator = null;

  @Nonnull
  public ItineraryFilterParameters itineraryFilters = ItineraryFilterParameters.createDefault();

  /**
   * The filled request parameters for penalties and thresholds values
   */
  public DataOverlayParameters dataOverlay = null;
  /**
   * Raptor can print all events when arriving at stops to system error. For developers only.
   */
  public DebugRaptor raptorDebugging = new DebugRaptor();

  /**
   * Set of options to use with Raptor. These are available here for testing purposes.
   */
  public RaptorOptions raptorOptions = new RaptorOptions();

  /**
   * List of OTP request tags, these are used to cross-cutting concerns like logging and micrometer
   * tags. Currently, all tags are added to all the timer instances for this request.
   */
  public Set<RoutingTag> tags = Set.of();

  private Envelope fromEnvelope;

  private Envelope toEnvelope;

  /* CONSTRUCTORS */

  /** Constructor for options; modes defaults to walk and transit */
  public RoutingRequest() {
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

  /* ACCESSOR/SETTER METHODS */

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

  public void setWheelchairAccessible(boolean wheelchair) {
    this.wheelchairAccessibility = this.wheelchairAccessibility.withEnabled(wheelchair);
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
    } else {
      this.walkBoardCost = walkBoardCost;
    }
  }

  public void setBikeBoardCost(int bikeBoardCost) {
    if (bikeBoardCost < 0) {
      this.bikeBoardCost = 0;
    } else {
      this.bikeBoardCost = bikeBoardCost;
    }
  }

  public void setPreferredAgencies(Collection<FeedScopedId> ids) {
    if (ids != null) {
      preferredAgencies = Set.copyOf(ids);
    }
  }

  public void setPreferredAgenciesFromString(String s) {
    if (!s.isEmpty()) {
      preferredAgencies = FeedScopedId.parseSetOfIds(s);
    }
  }

  public void setUnpreferredAgencies(Collection<FeedScopedId> ids) {
    if (ids != null) {
      unpreferredAgencies = Set.copyOf(ids);
    }
  }

  public void setUnpreferredAgenciesFromString(String s) {
    if (!s.isEmpty()) {
      unpreferredAgencies = FeedScopedId.parseSetOfIds(s);
    }
  }

  public void setBannedAgencies(Collection<FeedScopedId> ids) {
    if (ids != null) {
      bannedAgencies = Set.copyOf(ids);
    }
  }

  public void setBannedAgenciesFromSting(String s) {
    if (!s.isEmpty()) {
      bannedAgencies = FeedScopedId.parseSetOfIds(s);
    }
  }

  public void setWhiteListedAgencies(Collection<FeedScopedId> ids) {
    if (ids != null) {
      whiteListedAgencies = Set.copyOf(ids);
    }
  }

  public void setWhiteListedAgenciesFromSting(String s) {
    if (!s.isEmpty()) {
      whiteListedAgencies = FeedScopedId.parseSetOfIds(s);
    }
  }

  public void setOtherThanPreferredRoutesPenalty(int penalty) {
    if (penalty < 0) penalty = 0;
    this.otherThanPreferredRoutesPenalty = penalty;
  }

  public void setPreferredRoutes(List<FeedScopedId> routeIds) {
    preferredRoutes = RouteMatcher.idMatcher(routeIds);
  }

  public void setPreferredRoutesFromString(String s) {
    if (!s.isEmpty()) {
      preferredRoutes = RouteMatcher.parse(s);
    } else {
      preferredRoutes = RouteMatcher.emptyMatcher();
    }
  }

  public void setUnpreferredRoutes(List<FeedScopedId> routeIds) {
    unpreferredRoutes = RouteMatcher.idMatcher(routeIds);
  }

  public void setUnpreferredRoutesFromString(String s) {
    if (!s.isEmpty()) {
      unpreferredRoutes = RouteMatcher.parse(s);
    } else {
      unpreferredRoutes = RouteMatcher.emptyMatcher();
    }
  }

  public void setBannedRoutes(List<FeedScopedId> routeIds) {
    bannedRoutes = RouteMatcher.idMatcher(routeIds);
  }

  public void setBannedRoutesFromString(String s) {
    if (!s.isEmpty()) {
      bannedRoutes = RouteMatcher.parse(s);
    } else {
      bannedRoutes = RouteMatcher.emptyMatcher();
    }
  }

  public void setWhiteListedRoutesFromString(String s) {
    if (!s.isEmpty()) {
      whiteListedRoutes = RouteMatcher.parse(s);
    } else {
      whiteListedRoutes = RouteMatcher.emptyMatcher();
    }
  }

  public void setWhiteListedRoutes(List<FeedScopedId> routeIds) {
    whiteListedRoutes = RouteMatcher.idMatcher(routeIds);
  }

  public void setBannedTrips(List<FeedScopedId> ids) {
    if (ids != null) {
      bannedTrips = Set.copyOf(ids);
    }
  }

  public void setBannedTripsFromString(String ids) {
    if (!ids.isEmpty()) {
      bannedTrips = FeedScopedId.parseSetOfIds(ids);
    }
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
   * The search time for the current request. If the client have moved to the next page then this is
   * the adjusted search time - the dateTime passed in is ignored and replaced with by a time from
   * the pageToken.
   */
  public Instant getDateTime() {
    return dateTime;
  }

  public void setDateTime(Instant dateTime) {
    this.dateTime = dateTime;
  }

  public void setDateTime(String date, String time, TimeZone tz) {
    Date dateObject = DateUtils.toDate(date, time, tz);
    setDateTime(dateObject == null ? Instant.now() : dateObject.toInstant());
  }

  /**
   * Is the trip originally planned withing the previous/next 15h?
   */
  public boolean isTripPlannedForNow() {
    return Duration.between(dateTime, Instant.now()).abs().toSeconds() < NOW_THRESHOLD_SEC;
  }

  public void setPageCursor(String pageCursor) {
    this.pageCursor = PageCursor.decode(pageCursor);
  }

  public SortOrder getItinerariesSortOrder() {
    if (pageCursor != null) {
      return pageCursor.originalSortOrder;
    }
    return arriveBy ? SortOrder.STREET_AND_DEPARTURE_TIME : SortOrder.STREET_AND_ARRIVAL_TIME;
  }

  /**
   * Adjust the 'dateTime' if the page cursor is set to "goto next/previous page". The date-time is
   * used for many things, for example finding the days to search, but the transit search is using
   * the cursor[if exist], not the date-time.
   */
  public void applyPageCursor() {
    if (pageCursor != null) {
      // We switch to "depart-after" search when paging next(lat==null). It does not make
      // sense anymore to keep the latest-arrival-time when going to the "next page".
      if (pageCursor.latestArrivalTime == null) {
        arriveBy = false;
      }
      setDateTime(arriveBy ? pageCursor.latestArrivalTime : pageCursor.earliestDepartureTime);
      modes.directMode = StreetMode.NOT_SET;
      LOG.debug("Request dateTime={} set from pageCursor.", dateTime);
    }
  }

  /**
   * When paging we must crop the list of itineraries in the right end according to the sorting of
   * the original search and according to the page cursor type (next or previous).
   * <p>
   * We need to flip the cropping and crop the head/start of the itineraries when:
   * <ul>
   * <li>Paging to the previous page for a {@code depart-after/sort-on-arrival-time} search.
   * <li>Paging to the next page for a {@code arrive-by/sort-on-departure-time} search.
   * </ul>
   */
  public boolean maxNumberOfItinerariesCropHead() {
    if (pageCursor == null) {
      return false;
    }

    var previousPage = pageCursor.type == PageType.PREVIOUS_PAGE;
    return pageCursor.originalSortOrder.isSortedByArrivalTimeAcceding() == previousPage;
  }

  /**
   * Related to {@link #maxNumberOfItinerariesCropHead()}, but is {@code true} if we should crop the
   * search-window head(in the beginning) or tail(in the end).
   * <p>
   * For the first search we look if the sort is ascending(crop tail) or descending(crop head), and
   * for paged results we look at the paging type: next(tail) and previous(head).
   */
  public boolean doCropSearchWindowAtTail() {
    if (pageCursor == null) {
      return getItinerariesSortOrder().isSortedByArrivalTimeAcceding();
    }
    return pageCursor.type == PageType.NEXT_PAGE;
  }

  public void setNumItineraries(int numItineraries) {
    this.numItineraries = numItineraries;
  }

  public String toString(String sep) {
    return (
      from +
      sep +
      to +
      sep +
      dateTime +
      sep +
      arriveBy +
      sep +
      bicycleOptimizeType +
      sep +
      streetSubRequestModes.getAsStr()
    );
  }

  public void removeMode(TraverseMode mode) {
    streetSubRequestModes.setMode(mode, false);
  }

  /**
   * Sets intermediatePlaces by parsing GenericLocations from a list of string.
   */
  public void setIntermediatePlacesFromStrings(List<String> intermediates) {
    this.intermediatePlaces = new ArrayList<>(intermediates.size());
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

  /* INSTANCE METHODS */

  /**
   * Returns true if there are any intermediate places set.
   */
  public boolean hasIntermediatePlaces() {
    return this.intermediatePlaces != null && this.intermediatePlaces.size() > 0;
  }

  /**
   * Adds a GenericLocation to the end of the intermediatePlaces list. Will initialize
   * intermediatePlaces if it is null.
   */
  public void addIntermediatePlace(GenericLocation location) {
    if (this.intermediatePlaces == null) {
      this.intermediatePlaces = new ArrayList<>();
    }
    this.intermediatePlaces.add(location);
  }

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
          streetRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK)
          );
          streetRequest.parkAndRide = true;
          break;
        case BIKE_RENTAL:
          streetRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK)
          );
          streetRequest.vehicleRental = true;
          streetRequest.allowedRentalFormFactors.add(FormFactor.BICYCLE);
          break;
        case SCOOTER_RENTAL:
          streetRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK)
          );
          streetRequest.vehicleRental = true;
          streetRequest.allowedRentalFormFactors.add(FormFactor.SCOOTER);
          break;
        case CAR:
          streetRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.CAR));
          break;
        case CAR_TO_PARK:
          streetRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK)
          );
          streetRequest.parkAndRide = true;
          break;
        case CAR_PICKUP:
          streetRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK)
          );
          streetRequest.carPickup = true;
          break;
        case CAR_RENTAL:
          streetRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK)
          );
          streetRequest.vehicleRental = true;
          streetRequest.allowedRentalFormFactors.add(FormFactor.CAR);
      }
    }

    return streetRequest;
  }

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

      clone.bannedTrips = Set.copyOf(bannedTrips);

      clone.allowedRentalFormFactors = new HashSet<>(allowedRentalFormFactors);

      clone.raptorOptions = new RaptorOptions(this.raptorOptions);
      clone.raptorDebugging = new DebugRaptor(this.raptorDebugging);
      clone.itineraryFilters = new ItineraryFilterParameters(this.itineraryFilters);

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  public String toString() {
    return toString(" ");
  }

  public RoutingRequest reversedClone() {
    RoutingRequest ret = this.clone();
    ret.setArriveBy(!ret.arriveBy);
    ret.useVehicleRentalAvailabilityInformation = false;
    return ret;
  }

  /**
   * The road speed for a specific traverse mode.
   */
  public double getSpeed(TraverseMode mode, boolean walkingBike) {
    return switch (mode) {
      case WALK -> walkingBike ? bikeWalkingSpeed : walkSpeed;
      case BICYCLE -> bikeSpeed;
      case CAR -> carSpeed;
      default -> throw new IllegalArgumentException("getSpeed(): Invalid mode " + mode);
    };
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
    if (
      bannedRoutes.isEmpty() &&
      bannedAgencies.isEmpty() &&
      whiteListedRoutes.isEmpty() &&
      whiteListedAgencies.isEmpty()
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

  public Duration getMaxAccessEgressDuration(StreetMode mode) {
    return maxAccessEgressDurationForMode.getOrDefault(mode, maxAccessEgressDuration);
  }

  public Duration getMaxDirectStreetDuration(StreetMode mode) {
    return maxDirectStreetDurationForMode.getOrDefault(mode, maxDirectStreetDuration);
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
    boolean isUnpreferedRoute = unpreferredRoutes.matches(route);
    boolean isUnpreferedAgency = unpreferredAgencies.contains(agencyID);
    if (isUnpreferedRoute || isUnpreferedAgency) {
      preferences_penalty += useUnpreferredRoutesPenalty;
    }
    return preferences_penalty;
  }

  /**
   * Sets the bicycle triangle routing parameters -- the relative importance of safety, flatness,
   * and speed. These three fields of the RoutingRequest should have values between 0 and 1, and
   * should add up to 1. This setter function accepts any three numbers and will normalize them to
   * add up to 1.
   */
  public void setTriangleNormalized(double safe, double slope, double time) {
    if (safe == 0 && slope == 0 && time == 0) {
      var oneThird = 1f / 3;
      safe = oneThird;
      slope = oneThird;
      time = oneThird;
    }
    safe = setMinValue(safe);
    slope = setMinValue(slope);
    time = setMinValue(time);

    double total = safe + slope + time;
    if (total != 1) {
      LOG.warn(
        "Bicycle triangle factors don't add up to 1. Values will be scaled proportionally to each other."
      );
    }

    safe /= total;
    slope /= total;
    time /= total;
    this.bikeTriangleSafetyFactor = safe;
    this.bikeTriangleSlopeFactor = slope;
    this.bikeTriangleTimeFactor = time;
  }

  public Comparator<GraphPath> getPathComparator(boolean compareStartTimes) {
    if ("duration".equals(pathComparator)) {
      return new DurationComparator();
    }
    return new PathComparator(compareStartTimes);
  }

  /**
   * Returns if the vertex is considered "close" to the start or end point of the request. This is
   * useful if you want to allow loops in car routes under certain conditions.
   * <p>
   * Note: If you are doing Raptor access/egress searches this method does not take the possible
   * intermediate points (stations) into account. This means that stations might be skipped because
   * a car route to it cannot be found and a suboptimal route to another station is returned
   * instead.
   * <p>
   * If you encounter a case of this, you can adjust this code to take this into account.
   *
   * @see RoutingRequest#MAX_CLOSENESS_METERS
   * @see DominanceFunction#betterOrEqualAndComparable(State, State)
   */
  public boolean isCloseToStartOrEnd(Vertex vertex) {
    if (from == null || to == null || from.getCoordinate() == null || to.getCoordinate() == null) {
      return false;
    }
    if (fromEnvelope == null) {
      fromEnvelope = getEnvelope(from.getCoordinate(), MAX_CLOSENESS_METERS);
    }
    if (toEnvelope == null) {
      toEnvelope = getEnvelope(to.getCoordinate(), MAX_CLOSENESS_METERS);
    }
    return (
      fromEnvelope.intersects(vertex.getCoordinate()) ||
      toEnvelope.intersects(vertex.getCoordinate())
    );
  }

  private static Envelope getEnvelope(Coordinate c, int meters) {
    double lat = SphericalDistanceLibrary.metersToDegrees(meters);
    double lon = SphericalDistanceLibrary.metersToLonDegrees(meters, c.y);

    Envelope env = new Envelope(c);
    env.expandBy(lon, lat);

    return env;
  }

  /**
   * Checks if the route is banned. Also, if whitelisting is used, the route (or its agency) has to
   * be whitelisted in order to not count as banned.
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

  private double setMinValue(double value) {
    return Math.max(0, value);
  }
}
