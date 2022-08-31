package org.opentripplanner.api.common;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters;
import org.opentripplanner.model.plan.pagecursor.PageCursor;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class defines all the JAX-RS query parameters for a path search as fields, allowing them to
 * be inherited by other REST resource classes (the trip planner and the Analyst WMS or tile
 * resource). They will be properly included in API docs generated by Enunciate. This implies that
 * the concrete REST resource subclasses will be request-scoped rather than singleton-scoped.
 * <p>
 * All defaults should be specified in the RoutingRequest, NOT as annotations on the query
 * parameters. JSON router configuration can then overwrite those built-in defaults, and only the
 * fields of the resulting prototype routing request for which query parameters are found are
 * overwritten here. This establishes a priority chain: RoutingRequest field initializers, then JSON
 * router config, then query parameters.
 *
 * @author abyrd
 */
@SuppressWarnings({ "FieldMayBeFinal", "unused" })
public abstract class RoutingResource {

  private static final Logger LOG = LoggerFactory.getLogger(RoutingResource.class);

  /**
   * The start location -- either latitude, longitude pair in degrees or a Vertex label. For
   * example, <code>40.714476,-74.005966</code> or
   * <code>mtanyctsubway_A27_S</code>.
   */
  @QueryParam("fromPlace")
  protected String fromPlace;

  /** The end location (see fromPlace for format). */
  @QueryParam("toPlace")
  protected String toPlace;

  /**
   * An ordered list of intermediate locations to be visited (see the fromPlace for format).
   * Parameter can be specified multiple times.
   *
   * @deprecated TODO OTP2 - Regression. Not currently working in OTP2. Must be re-implemented
   * - using raptor.
   */
  @Deprecated
  @QueryParam("intermediatePlaces")
  protected List<String> intermediatePlaces;

  /** The date that the trip should depart (or arrive, for requests where arriveBy is true). */
  @QueryParam("date")
  protected String date;

  /** The time that the trip should depart (or arrive, for requests where arriveBy is true). */
  @QueryParam("time")
  protected String time;

  /**
   * The length of the search-window in seconds. This parameter is optional.
   * <p>
   * The search-window is defined as the duration between the earliest-departure-time(EDT) and the
   * latest-departure-time(LDT). OTP will search for all itineraries in this departure window. If
   * {@code arriveBy=true} the {@code dateTime} parameter is the latest-arrival-time, so OTP will
   * dynamically calculate the EDT. Using a short search-window is faster than using a longer one,
   * but the search duration is not linear. Using a \"too\" short search-window will waste resources
   * server side, while using a search-window that is too long will be slow.
   * <p>
   * OTP will dynamically calculate a reasonable value for the search-window, if not provided. The
   * calculation comes with a significant overhead (10-20% extra). Whether you should use the
   * dynamic calculated value or pass in a value depends on your use-case. For a travel planner in a
   * small geographical area, with a dense network of public transportation, a fixed value between
   * 40 minutes and 2 hours makes sense. To find the appropriate search-window, adjust it so that
   * the number of itineraries on average is around the wanted {@code numItineraries}. Make sure you
   * set the {@code numItineraries} to a high number while testing. For a country wide area like
   * Norway, using the dynamic search-window is the best.
   * <p>
   * When paginating, the search-window is calculated using the {@code numItineraries} in the
   * original search together with statistics from the search for the last page. This behaviour is
   * configured server side, and can not be overridden from the client.
   * <p>
   * The search-window used is returned to the response metadata as {@code searchWindowUsed} for
   * debugging purposes.
   */
  @QueryParam("searchWindow")
  protected Integer searchWindow;

  /**
   * Use the cursor to go to the next "page" of itineraries. Copy the cursor from the last response
   * and keep the original request as is. This will enable you to search for itineraries in the next
   * or previous time-window.
   * <p>
   * This is an optional parameter.
   */
  @QueryParam("pageCursor")
  public String pageCursor;

  /**
   * Search for the best trip options within a time window. If {@code true} two itineraries are
   * considered optimal if one is better on arrival time(earliest wins) and the other is better on
   * departure time(latest wins).
   * <p>
   * In combination with {@code arriveBy} this parameter cover the following 3 use cases:
   * <ul>
   *   <li>
   *     Traveler want to find thee best alternative within a time window. Set
   *     {@code timetableView=true} and {@code arriveBy=false}.  This is the default, and if
   *     the intention of the traveler is unknown it gives the best result, because it includes
   *     the two next use-cases. Setting the {@code arriveBy=false}, covers the same use-case,
   *     but the input time is interpreted as latest-arrival-time, and not
   *     earliest-departure-time. This works great with paging, request next/previous time-window.
   *   </li>
   *   <li>
   *     Traveler want to find the best alternative with departure after a specific time.
   *     For example: I am at the station now and want to get home as quickly as possible.
   *     Set {@code timetableView=false} and {@code arriveBy=false}. Do not support paging.
   *   </li>
   *   <li>
   *     Traveler want to find the best alternative with arrival before specific time. For
   *     example going to a meeting. Do not support paging.
   *     Set {@code timetableView=false} and {@code arriveBy=true}.
   *   </li>
   * </ul>
   * Default: true
   */
  @QueryParam("timetableView")
  public Boolean timetableView;

  /**
   * Whether the trip should depart or arrive at the specified date and time.
   *
   * @see #timetableView for usage.
   */
  @Deprecated
  @QueryParam("arriveBy")
  protected Boolean arriveBy;

  /**
   * Whether the trip must be wheelchair accessible.
   *
   * @deprecated TODO OTP2 Regression. Not currently working in OTP2. This is not implemented
   * in Raptor jet.
   */
  @Deprecated
  @QueryParam("wheelchair")
  protected Boolean wheelchair;

  /**
   * The maximum time (in seconds) of pre-transit travel when using drive-to-transit (park and ride
   * or kiss and ride). Defaults to unlimited.
   * <p>
   * See https://github.com/opentripplanner/OpenTripPlanner/issues/2886
   *
   * @deprecated TODO OTP2 - Regression. Not currently working in OTP2.
   */
  @Deprecated
  @QueryParam("maxPreTransitTime")
  protected Integer maxPreTransitTime;

  /**
   * A multiplier for how bad walking with a bike is, compared to being in transit for equal lengths
   * of time. Defaults to 3.
   */
  @QueryParam("bikeWalkingReluctance")
  protected Double bikeWalkingReluctance;

  /**
   * A multiplier for how bad walking is, compared to being in transit for equal
   * lengths of time. Empirically, values between 2 and 4 seem to correspond
   * well to the concept of not wanting to walk too much without asking for
   * totally ridiculous itineraries, but this observation should in no way be
   * taken as scientific or definitive. Your mileage may vary. See
   * https://github.com/opentripplanner/OpenTripPlanner/issues/4090 for impact on
   * performance with high values. Default value: 2.0
   */
  @QueryParam("walkReluctance")
  protected Double walkReluctance;

  @QueryParam("bikeReluctance")
  protected Double bikeReluctance;

  @QueryParam("carReluctance")
  protected Double carReluctance;

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
  @QueryParam("waitReluctance")
  protected Double waitReluctance;

  /** How much less bad is waiting at the beginning of the trip (replaces waitReluctance) */
  @QueryParam("waitAtBeginningFactor")
  protected Double waitAtBeginningFactor;

  /** The user's walking speed in meters/second. Defaults to approximately 3 MPH. */
  @QueryParam("walkSpeed")
  protected Double walkSpeed;

  /**
   * The user's biking speed in meters/second. Defaults to approximately 11 MPH, or 9.5 for
   * bikeshare.
   */
  @QueryParam("bikeSpeed")
  protected Double bikeSpeed;

  /** The user's bike walking speed in meters/second. Defaults to approximately 3 MPH. */
  @QueryParam("bikeWalkingSpeed")
  protected Double bikeWalkingSpeed;

  /**
   * The time it takes the user to fetch their bike and park it again in seconds. Defaults to 0.
   */
  @QueryParam("bikeSwitchTime")
  protected Integer bikeSwitchTime;

  /**
   * The cost of the user fetching their bike and parking it again. Defaults to 0.
   */
  @QueryParam("bikeSwitchCost")
  protected Integer bikeSwitchCost;

  /** For bike triangle routing, how much safety matters (range 0-1). */
  @QueryParam("triangleSafetyFactor")
  protected Double triangleSafetyFactor;

  /** For bike triangle routing, how much slope matters (range 0-1). */
  @QueryParam("triangleSlopeFactor")
  protected Double triangleSlopeFactor;

  /** For bike triangle routing, how much time matters (range 0-1). */
  @QueryParam("triangleTimeFactor")
  protected Double triangleTimeFactor;

  /**
   * The set of characteristics that the user wants to optimize for. @See OptimizeType.
   *
   * @deprecated TODO OTP2 this should be completely removed and done only with individual cost
   * parameters
   * Also: apparently OptimizeType only affects BICYCLE mode traversal of
   * street segments. If this is the case it should be very well
   * documented and carried over into the Enum name.
   */
  @Deprecated
  @QueryParam("optimize")
  protected BicycleOptimizeType optimize;

  /**
   * The set of modes that a user is willing to use, with qualifiers stating whether vehicles should
   * be parked, rented, etc.
   * <p>
   * The possible values of the comma-separated list are:
   *
   * <ul>
   *  <li>WALK</li>
   *  <li>TRANSIT</li>
   *  <li>BICYCLE</li>
   *  <li>BICYCLE_RENT</li>
   *  <li>BICYCLE_PARK</li>
   *  <li>CAR</li>
   *  <li>CAR_PARK</li>
   *  <li>TRAM</li>
   *  <li>SUBWAY</li>
   *  <li>RAIL</li>
   *  <li>BUS</li>
   *  <li>CABLE_CAR</li>
   *  <li>FERRY</li>
   *  <li>GONDOLA</li>
   *  <li>FUNICULAR</li>
   *  <li>AIRPLANE</li>
   * </ul>
   * <p>
   *   For a more complete discussion of this parameter see
   *   <a href="http://docs.opentripplanner.org/en/latest/Configuration/#routing-modes">Routing modes</a>.
   */
  @QueryParam("mode")
  protected QualifiedModeSet modes;

  /**
   * The minimum time, in seconds, between successive trips on different vehicles. This is designed
   * to allow for imperfect schedule adherence. This is a minimum; transfers over longer distances
   * might use a longer time.
   *
   * @deprecated TODO OTP2: Needs to be implemented
   */
  @Deprecated
  @QueryParam("minTransferTime")
  protected Integer minTransferTime;

  /** The maximum number of possible itineraries to return. */
  @QueryParam("numItineraries")
  protected Integer numItineraries;

  /**
   * The comma-separated list of preferred agencies.
   *
   * @deprecated TODO OTP2: Needs to be implemented
   */
  @Deprecated
  @QueryParam("preferredAgencies")
  protected String preferredAgencies;

  /**
   * The comma-separated list of unpreferred agencies.
   *
   * @deprecated TODO OTP2: Needs to be implemented
   */
  @Deprecated
  @QueryParam("unpreferredAgencies")
  protected String unpreferredAgencies;

  /**
   * The comma-separated list of banned agencies.
   */
  @QueryParam("bannedAgencies")
  protected String bannedAgencies;

  /**
   * Functions the same as banned agencies, except only the listed agencies are allowed.
   */
  @QueryParam("whiteListedAgencies")
  protected String whiteListedAgencies;

  /**
   * Whether intermediate stops -- those that the itinerary passes in a vehicle, but does not board
   * or alight at -- should be returned in the response.  For example, on a Q train trip from
   * Prospect Park to DeKalb Avenue, whether 7th Avenue and Atlantic Avenue should be included.
   */
  @QueryParam("showIntermediateStops")
  @DefaultValue("false")
  protected Boolean showIntermediateStops;

  /**
   * Prevents unnecessary transfers by adding a cost for boarding a vehicle. This is the cost that
   * is used when boarding while walking.
   */
  @QueryParam("walkBoardCost")
  protected Integer walkBoardCost;

  /**
   * Prevents unnecessary transfers by adding a cost for boarding a vehicle. This is the cost that
   * is used when boarding while cycling. This is usually higher that walkBoardCost.
   */
  @QueryParam("bikeBoardCost")
  protected Integer bikeBoardCost;

  /**
   * Factor for how much the walk safety is considered in routing. Value should be between 0 and 1.
   * If the value is set to be 0, safety is ignored. Default is 1.0.
   */
  @QueryParam("walkSafetyFactor")
  protected Double walkSafetyFactor;

  @QueryParam("allowKeepingRentedBicycleAtDestination")
  protected Boolean allowKeepingRentedBicycleAtDestination;

  @QueryParam("keepingRentedBicycleAtDestinationCost")
  protected Double keepingRentedBicycleAtDestinationCost;

  /** The vehicle rental networks which may be used. If empty all networks may be used. */
  @QueryParam("allowedVehicleRentalNetworks")
  protected Set<String> allowedVehicleRentalNetworks;

  /** The vehicle rental networks which may not be used. If empty, no networks are banned. */
  @QueryParam("bannedVehicleRentalNetworks")
  protected Set<String> bannedVehicleRentalNetworks;

  /** Time to park a bike */
  @QueryParam("bikeParkTime")
  protected Integer bikeParkTime;

  /** Cost of parking a bike. */
  @QueryParam("bikeParkCost")
  protected Integer bikeParkCost;

  /** Time to park a car */
  @QueryParam("carParkTime")
  protected Integer carParkTime = 60;

  /** Cost of parking a car. */
  @QueryParam("carParkCost")
  protected Integer carParkCost = 120;

  /** Tags which are required to use a vehicle parking. If empty, no tags are required. */
  @QueryParam("requiredVehicleParkingTags")
  protected Set<String> requiredVehicleParkingTags = Set.of();

  /** Tags with which a vehicle parking will not be used. If empty, no tags are banned. */
  @QueryParam("bannedVehicleParkingTags")
  protected Set<String> bannedVehicleParkingTags = Set.of();

  /**
   * The comma-separated list of banned routes. The format is agency_[routename][_routeid], so
   * TriMet_100 (100 is route short name) or Trimet__42 (two underscores, 42 is the route internal
   * ID).
   */
  @Deprecated
  @QueryParam("bannedRoutes")
  protected String bannedRoutes;

  /**
   * Functions the same as bannnedRoutes, except only the listed routes are allowed.
   */
  @QueryParam("whiteListedRoutes")
  @Deprecated
  protected String whiteListedRoutes;

  /**
   * The list of preferred routes. The format is agency_[routename][_routeid], so TriMet_100 (100 is
   * route short name) or Trimet__42 (two underscores, 42 is the route internal ID).
   *
   * @deprecated TODO OTP2 Needs to be implemented
   */
  @Deprecated
  @QueryParam("preferredRoutes")
  protected String preferredRoutes;

  /**
   * The list of unpreferred routes. The format is agency_[routename][_routeid], so TriMet_100 (100
   * is route short name) or Trimet__42 (two underscores, 42 is the route internal ID).
   *
   * @deprecated TODO OTP2 Needs to be implemented
   */
  @Deprecated
  @QueryParam("unpreferredRoutes")
  protected String unpreferredRoutes;

  /**
   * Penalty added for using every route that is not preferred if user set any route as preferred,
   * i.e. number of seconds that we are willing to wait for preferred route.
   *
   * @deprecated TODO OTP2 Needs to be implemented
   */
  @Deprecated
  @QueryParam("otherThanPreferredRoutesPenalty")
  protected Integer otherThanPreferredRoutesPenalty;

  /**
   * The comma-separated list of banned trips.  The format is feedId:tripId
   */
  @QueryParam("bannedTrips")
  protected String bannedTrips;

  /**
   * A comma-separated list of banned stops. A stop is banned by ignoring its pre-board and
   * pre-alight edges. This means the stop will be reachable via the street network. Also, it is
   * still possible to travel through the stop. Just boarding and alighting is prohibited. The
   * format is agencyId_stopId, so: TriMet_2107
   *
   * @deprecated TODO OTP2 This no longer works in OTP2, see issue #2843.
   */
  @Deprecated
  @QueryParam("bannedStops")
  protected String bannedStops;

  /**
   * A comma-separated list of banned stops. A stop is banned by ignoring its pre-board and
   * pre-alight edges. This means the stop will be reachable via the street network. It is not
   * possible to travel through the stop. For example, this parameter can be used when a train
   * station is destroyed, such that no trains can drive through the station anymore. The format is
   * agencyId_stopId, so: TriMet_2107
   *
   * @deprecated TODO OTP2 This no longer works in OTP2, see issue #2843.
   */
  @Deprecated
  @QueryParam("bannedStopsHard")
  protected String bannedStopsHard;

  /**
   * An additional penalty added to boardings after the first. The value is in OTP's internal weight
   * units, which are roughly equivalent to seconds.  Set this to a high value to discourage
   * transfers.  Of course, transfers that save significant time or walking will still be taken.
   */
  @QueryParam("transferPenalty")
  protected Integer transferPenalty;

  /**
   * An additional penalty added to boardings after the first when the transfer is not preferred.
   * Preferred transfers also include timed transfers. The value is in OTP's internal weight units,
   * which are roughly equivalent to seconds. Set this to a high value to discourage transfers that
   * are not preferred. Of course, transfers that save significant time or walking will still be
   * taken. When no preferred or timed transfer is defined, this value is ignored.
   * <p>
   * TODO OTP2 This JavaDoc needs clarification. What is a "preferred" Transfer, the GTFS
   *           specification do not have "preferred Transfers". The GTFS spec transfer
   *           type 0 is _Recommended transfer point_ - is this what is meant?
   *
   * @deprecated TODO OTP2 Regression. Not currently working in OTP2. We might not implement the
   * old functionality the same way, but we will try to map this parameter
   * so it does work similar as before.
   */
  @Deprecated
  @QueryParam("nonpreferredTransferPenalty")
  protected Integer nonpreferredTransferPenalty;

  /**
   * The maximum number of transfers (that is, one plus the maximum number of boardings) that a trip
   * will be allowed.
   * <p>
   * Consider using the {@link #transferPenalty} instead of this parameter.
   * <p>
   * See https://github.com/opentripplanner/OpenTripPlanner/issues/2886
   *
   * @deprecated TODO OTP2 Regression. A maxTransfers should be set in the router config, not
   * here. Instead the client should be able to pass in a parameter for
   * the max number of additional/extra transfers relative to the best
   * trip (with the fewest possible transfers) within constraint of the
   * other search parameters.
   * This might be to complicated to explain to the customer, so we
   * might stick to the old limit, but that have side-effects that you
   * might not find any trips on a day where a critical part of the
   * trip is not available, because of some real-time disruption.
   */
  @Deprecated
  @QueryParam("maxTransfers")
  protected Integer maxTransfers;

  /**
   * If true, goal direction is turned off and a full path tree is built (specify only once)
   *
   * @Deprecated - This is not supported in OTP2 any more.
   */
  @Deprecated
  @QueryParam("batch")
  protected Boolean batch;

  /**
   * A transit stop required to be the first stop in the search (AgencyId_StopId)
   *
   * @deprecated TODO OTP2 Is this in use, what is is used for. It seems to overlap with
   * the fromPlace parameter. Is is used for onBoard routing only?
   */
  @Deprecated
  @QueryParam("startTransitStopId")
  protected String startTransitStopId;

  /**
   * A transit trip acting as a starting "state" for depart-onboard routing (AgencyId_TripId)
   *
   * @deprecated TODO OTP2 Regression. Not currently working in OTP2. We might not implement the
   * old functionality the same way, but we will try to map this parameter
   * so it does work similar as before.
   */
  @Deprecated
  @QueryParam("startTransitTripId")
  protected String startTransitTripId;

  /**
   * When subtracting initial wait time, do not subtract more than this value, to prevent overly
   * optimistic trips. Reasoning is that it is reasonable to delay a trip start 15 minutes to make a
   * better trip, but that it is not reasonable to delay a trip start 15 hours; if that is to be
   * done, the time needs to be included in the trip time. This number depends on the transit
   * system; for transit systems where trips are planned around the vehicles, this number can be
   * much higher. For instance, it's perfectly reasonable to delay one's trip 12 hours if one is
   * taking a cross-country Amtrak train from Emeryville to Chicago. Has no effect in stock OTP,
   * only in Analyst.
   * <p>
   * A value of 0 means that initial wait time will not be subtracted out (will be clamped to 0). A
   * value of -1 (the default) means that clamping is disabled, so any amount of initial wait time
   * will be subtracted out.
   *
   * @deprecated This parameter is not in use any more.
   */
  @Deprecated
  @QueryParam("clampInitialWait")
  protected Long clampInitialWait;

  /**
   * THIS PARAMETER IS NO LONGER IN USE.
   * <p>
   * If true, this trip will be reverse-optimized on the fly. Otherwise, reverse-optimization will
   * occur once a trip has been chosen (in Analyst, it will not be done at all).
   *
   * @deprecated This parameter is not in use any more after the transit search switched from AStar
   * to Raptor.
   */
  @Deprecated
  @QueryParam("reverseOptimizeOnTheFly")
  protected Boolean reverseOptimizeOnTheFly;

  /**
   * The number of seconds to add before boarding a transit leg. It is recommended to use the {@code
   * boardTimes} in the {@code router-config.json} to set this for each mode.
   * <p>
   * Unit is seconds. Default value is 0.
   */
  @QueryParam("boardSlack")
  private Integer boardSlack;

  /**
   * The number of seconds to add after alighting a transit leg. It is recommended to use the {@code
   * alightTimes} in the {@code router-config.json} to set this for each mode.
   * <p>
   * Unit is seconds. Default value is 0.
   */
  @QueryParam("alightSlack")
  private Integer alightSlack;

  @QueryParam("locale")
  private String locale;

  /**
   * If true, realtime updates are ignored during this search.
   *
   * @deprecated TODO OTP2 Regression. Not currently working in OTP2.
   */
  @Deprecated
  @QueryParam("ignoreRealtimeUpdates")
  protected Boolean ignoreRealtimeUpdates;

  /**
   * If true, the remaining weight heuristic is disabled. Currently only implemented for the long
   * distance path service.
   */
  @QueryParam("disableRemainingWeightHeuristic")
  protected Boolean disableRemainingWeightHeuristic;

  /**
   * See https://github.com/opentripplanner/OpenTripPlanner/issues/2886
   *
   * @deprecated TODO OTP2 This is not useful as a search parameter, but could be used as a
   * post search filter to reduce number of itineraries down to an
   * acceptable number, but there are probably better ways to do that.
   */
  @Deprecated
  @QueryParam("maxHours")
  private Double maxHours;

  /**
   * See https://github.com/opentripplanner/OpenTripPlanner/issues/2886
   *
   * @deprecated see {@link #maxHours}
   */
  @QueryParam("useRequestedDateTimeInMaxHours")
  @Deprecated
  private Boolean useRequestedDateTimeInMaxHours;

  @QueryParam("disableAlertFiltering")
  private Boolean disableAlertFiltering;

  @QueryParam("debugItineraryFilter")
  private Boolean debugItineraryFilter;

  /**
   * If true, the Graph's ellipsoidToGeoidDifference is applied to all elevations returned by this
   * query.
   */
  @QueryParam("geoidElevation")
  private Boolean geoidElevation;

  /**
   * Set the method of sorting itineraries in the response. Right now, the only supported value is
   * "duration"; otherwise it uses default sorting. More sorting methods may be added in the
   * future.
   *
   * @deprecated TODO OTP2 Regression. Not currently working in OTP2 at the moment.
   */
  @Deprecated
  @QueryParam("pathComparator")
  private String pathComparator;

  @QueryParam("useVehicleParkingAvailabilityInformation")
  private Boolean useVehicleParkingAvailabilityInformation;

  @QueryParam("debugRaptorStops")
  private String debugRaptorStops;

  @QueryParam("debugRaptorPath")
  private String debugRaptorPath;

  /**
   * somewhat ugly bug fix: the graphService is only needed here for fetching per-graph time zones.
   * this should ideally be done when setting the routing context, but at present departure/ arrival
   * time is stored in the request as an epoch time with the TZ already resolved, and other code
   * depends on this behavior. (AMB) Alternatively, we could eliminate the separate RoutingRequest
   * objects and just resolve vertices and timezones here right away, but just ignore them in
   * semantic equality checks.
   */
  @Context
  protected OtpServerRequestContext serverContext;

  /**
   * Range/sanity check the query parameter fields and build a Request object from them.
   *
   * @param queryParameters incoming request parameters
   */
  protected RouteRequest buildRequest(MultivaluedMap<String, String> queryParameters) {
    RouteRequest request = serverContext.defaultRoutingRequest();

    // The routing request should already contain defaults, which are set when it is initialized or
    // in the JSON router configuration and cloned. We check whether each parameter was supplied
    // before overwriting the default.
    if (fromPlace != null) request.from = LocationStringParser.fromOldStyleString(fromPlace);

    if (toPlace != null) request.to = LocationStringParser.fromOldStyleString(toPlace);

    {
      //FIXME: move into setter method on routing request
      ZoneId tz = serverContext.transitService().getTimeZone();
      if (date == null && time != null) { // Time was provided but not date
        LOG.debug("parsing ISO datetime {}", time);
        try {
          // If the time query param doesn't specify a timezone, use the graph's default. See issue #1373.
          DatatypeFactory df = javax.xml.datatype.DatatypeFactory.newInstance();
          XMLGregorianCalendar xmlGregCal = df.newXMLGregorianCalendar(time);
          ZonedDateTime dateTime = xmlGregCal.toGregorianCalendar().toZonedDateTime();
          if (xmlGregCal.getTimezone() == DatatypeConstants.FIELD_UNDEFINED) {
            dateTime = dateTime.withZoneSameLocal(tz);
          }
          request.setDateTime(dateTime.toInstant());
        } catch (DatatypeConfigurationException e) {
          request.setDateTime(date, time, tz);
        }
      } else {
        request.setDateTime(date, time, tz);
      }
    }

    if (searchWindow != null) {
      request.searchWindow = Duration.ofSeconds(searchWindow);
    }
    if (pageCursor != null) {
      request.pageCursor = PageCursor.decode(pageCursor);
    }
    if (timetableView != null) {
      request.timetableView = timetableView;
    }

    if (wheelchair != null) request.setWheelchairAccessible(wheelchair);

    if (numItineraries != null) request.setNumItineraries(numItineraries);

    if (bikeReluctance != null) request.setBikeReluctance(bikeReluctance);

    if (bikeWalkingReluctance != null) request.setBikeWalkingReluctance(bikeWalkingReluctance);

    if (carReluctance != null) request.setCarReluctance(carReluctance);

    if (walkReluctance != null) request.setWalkReluctance(walkReluctance);

    if (waitReluctance != null) request.setWaitReluctance(waitReluctance);

    if (waitAtBeginningFactor != null) request.setWaitAtBeginningFactor(waitAtBeginningFactor);

    if (walkSpeed != null) request.walkSpeed = walkSpeed;

    if (bikeSpeed != null) request.bikeSpeed = bikeSpeed;

    if (bikeWalkingSpeed != null) request.bikeWalkingSpeed = bikeWalkingSpeed;

    if (bikeSwitchTime != null) request.bikeSwitchTime = bikeSwitchTime;

    if (bikeSwitchCost != null) request.bikeSwitchCost = bikeSwitchCost;

    if (
      allowKeepingRentedBicycleAtDestination != null
    ) request.allowKeepingRentedVehicleAtDestination = allowKeepingRentedBicycleAtDestination;

    if (
      keepingRentedBicycleAtDestinationCost != null
    ) request.keepingRentedVehicleAtDestinationCost = keepingRentedBicycleAtDestinationCost;

    if (allowedVehicleRentalNetworks != null) request.allowedVehicleRentalNetworks =
      allowedVehicleRentalNetworks;

    if (bannedVehicleRentalNetworks != null) request.bannedVehicleRentalNetworks =
      bannedVehicleRentalNetworks;

    if (bikeParkCost != null) request.bikeParkCost = bikeParkCost;

    if (bikeParkTime != null) request.bikeParkTime = bikeParkTime;

    if (carParkCost != null) request.carParkCost = carParkCost;

    if (carParkTime != null) request.carParkTime = carParkTime;

    if (bannedVehicleParkingTags != null) request.bannedVehicleParkingTags =
      bannedVehicleParkingTags;

    if (requiredVehicleParkingTags != null) request.requiredVehicleParkingTags =
      requiredVehicleParkingTags;

    if (optimize != null) {
      // Optimize types are basically combined presets of routing parameters, except for triangle
      request.setBicycleOptimizeType(optimize);
      if (optimize == BicycleOptimizeType.TRIANGLE) {
        request.setTriangleNormalized(
          triangleSafetyFactor,
          triangleSlopeFactor,
          triangleTimeFactor
        );
      }
    }

    if (arriveBy != null) {
      request.setArriveBy(arriveBy);
    }
    if (intermediatePlaces != null) {
      request.setIntermediatePlacesFromStrings(intermediatePlaces);
    }
    if (preferredRoutes != null) {
      request.setPreferredRoutesFromString(preferredRoutes);
    }
    if (otherThanPreferredRoutesPenalty != null) {
      request.setOtherThanPreferredRoutesPenalty(otherThanPreferredRoutesPenalty);
    }
    if (preferredAgencies != null) {
      request.setPreferredAgenciesFromString(preferredAgencies);
    }
    if (unpreferredRoutes != null) {
      request.setUnpreferredRoutesFromString(unpreferredRoutes);
    }
    if (unpreferredAgencies != null) {
      request.setUnpreferredAgenciesFromString(unpreferredAgencies);
    }
    if (walkBoardCost != null) {
      request.setWalkBoardCost(walkBoardCost);
    }
    if (bikeBoardCost != null) {
      request.setBikeBoardCost(bikeBoardCost);
    }
    if (walkSafetyFactor != null) {
      request.setWalkSafetyFactor(walkSafetyFactor);
    }
    if (bannedRoutes != null) {
      request.setBannedRoutesFromString(bannedRoutes);
    }
    if (whiteListedRoutes != null) {
      request.setWhiteListedRoutesFromString(whiteListedRoutes);
    }
    if (bannedAgencies != null) {
      request.setBannedAgenciesFromSting(bannedAgencies);
    }
    if (whiteListedAgencies != null) {
      request.setWhiteListedAgenciesFromSting(whiteListedAgencies);
    }
    if (bannedTrips != null) {
      request.setBannedTripsFromString(bannedTrips);
    }
    // The "Least transfers" optimization is accomplished via an increased transfer penalty.
    // See comment on RoutingRequest.transferPentalty.
    if (transferPenalty != null) {
      request.transferCost = transferPenalty;
    }

    if (optimize != null) {
      request.setBicycleOptimizeType(optimize);
    }
    /* Temporary code to get bike/car parking and renting working. */
    if (modes != null && !modes.qModes.isEmpty()) {
      request.modes = modes.getRequestModes();
    }

    if (request.vehicleRental && bikeSpeed == null) {
      //slower bike speed for bike sharing, based on empirical evidence from DC.
      request.bikeSpeed = 4.3;
    }

    if (boardSlack != null) request.boardSlack = boardSlack;

    if (alightSlack != null) request.alightSlack = alightSlack;

    if (minTransferTime != null) {
      int alightAndBoardSlack = request.boardSlack + request.alightSlack;
      if (alightAndBoardSlack > minTransferTime) {
        throw new IllegalArgumentException(
          "Invalid parameters: 'minTransferTime' must be greater than or equal to board slack plus alight slack"
        );
      }
      request.transferSlack = minTransferTime - alightAndBoardSlack;
    }

    if (nonpreferredTransferPenalty != null) request.nonpreferredTransferCost =
      nonpreferredTransferPenalty;

    if (maxTransfers != null) request.maxTransfers = maxTransfers;

    request.useVehicleRentalAvailabilityInformation = request.isTripPlannedForNow();

    if (startTransitStopId != null && !startTransitStopId.isEmpty()) request.startingTransitStopId =
      FeedScopedId.parseId(startTransitStopId);

    if (startTransitTripId != null && !startTransitTripId.isEmpty()) request.startingTransitTripId =
      FeedScopedId.parseId(startTransitTripId);

    if (ignoreRealtimeUpdates != null) request.ignoreRealtimeUpdates = ignoreRealtimeUpdates;

    if (disableAlertFiltering != null) request.disableAlertFiltering = disableAlertFiltering;

    if (geoidElevation != null) request.geoidElevation = geoidElevation;

    if (pathComparator != null) request.pathComparator = pathComparator;

    if (debugItineraryFilter != null) {
      request.itineraryFilters.debug = debugItineraryFilter;
    }

    request.raptorDebugging.withStops(debugRaptorStops).withPath(debugRaptorPath);

    if (useVehicleParkingAvailabilityInformation != null) {
      request.useVehicleParkingAvailabilityInformation = useVehicleParkingAvailabilityInformation;
    }

    if (locale != null) {
      request.locale = Locale.forLanguageTag(locale.replaceAll("-", "_"));
    }

    if (OTPFeature.DataOverlay.isOn()) {
      var queryDataOverlayParameters = DataOverlayParameters.parseQueryParams(queryParameters);
      if (!queryDataOverlayParameters.isEmpty()) {
        request.dataOverlay = queryDataOverlayParameters;
      }
    }

    return request;
  }
}
