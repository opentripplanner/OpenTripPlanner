package org.opentripplanner.routing.api.request;

import static org.opentripplanner.util.time.DurationUtils.durationInSeconds;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.common.LocationStringParser;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.pagecursor.PageCursor;
import org.opentripplanner.model.plan.pagecursor.PageType;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairAccessibilityPreferences;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.util.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A trip planning request. Some parameters may not be honored by the trip planner for some or all
 * itineraries.
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
public class RouteRequest implements Cloneable, Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(RouteRequest.class);

  private static final long NOW_THRESHOLD_SEC = durationInSeconds("15h");

  /* FIELDS UNIQUELY IDENTIFYING AN SPT REQUEST */
  /**
   * How close to do you have to be to the start or end to be considered "close".
   *
   * @see RouteRequest#isCloseToStartOrEnd(Vertex)
   * @see DominanceFunction#betterOrEqualAndComparable(State, State)
   */
  private static final int MAX_CLOSENESS_METERS = 500;
  /** The complete list of incoming query parameters. */

  private GenericLocation from;

  private GenericLocation to;

  /**
   * An ordered list of intermediate locations to be visited.
   *
   * @deprecated TODO OTP2 - Regression. Not currently working in OTP2. Must be re-implemented
   * - using raptor.
   */
  @Deprecated
  public List<GenericLocation> intermediatePlaces;

  /**
   * The access/egress/direct/transit modes allowed for this main request. The parameter
   * "streetSubRequestModes" below is used for a single A Star sub request.
   * <p>
   * // TODO OTP2 Street routing requests should eventually be split into its own request class.
   */
  public RequestModes modes = RequestModes.defaultRequestModes();
  /**
   * The set of TraverseModes allowed when doing creating sub requests and doing street routing. //
   * TODO OTP2 Street routing requests should eventually be split into its own request class.
   */
  public TraverseModeSet streetSubRequestModes = new TraverseModeSet(TraverseMode.WALK); // defaults in constructor overwrite this

  private Instant dateTime = Instant.now();

  private Duration searchWindow;

  private PageCursor pageCursor;

  private boolean timetableView = true;

  private boolean arriveBy = false;

  private int numItineraries = 50;

  private Locale locale = new Locale("en", "US");

  private RoutingPreferences preferences;

  /** The vehicle rental networks which may be used. If empty all networks may be used. */
  public Set<String> allowedVehicleRentalNetworks = Set.of();
  /** The vehicle rental networks which may not be used. If empty, no networks are banned. */
  public Set<String> bannedVehicleRentalNetworks = Set.of();
  /** Tags which are required to use a vehicle parking. If empty, no tags are required. */
  public Set<String> requiredVehicleParkingTags = Set.of();
  /** Tags with which a vehicle parking will not be used. If empty, no tags are banned. */
  public Set<String> bannedVehicleParkingTags = Set.of();

  private boolean wheelchair = false;

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
   * Set of preferred routes by user and configuration.
   *
   * @deprecated TODO OTP2 Needs to be implemented
   */
  @Deprecated
  public List<FeedScopedId> preferredRoutes = List.of();

  /**
   * Set of unpreferred routes for given user and configuration.
   */
  private Set<FeedScopedId> unpreferredRoutes = Set.of();

  /**
   * Do not use certain trips
   */
  public Set<FeedScopedId> bannedTrips = Set.of();
  /**
   * Whether arriving at the destination with a rented (station) bicycle is allowed without dropping
   * it off.
   *
   * @see RouteRequest#keepingRentedVehicleAtDestinationCost
   * @see VehicleRentalStation#isKeepingVehicleRentalAtDestinationAllowed
   */
  public boolean allowKeepingRentedVehicleAtDestination = false;

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
   * Raptor can print all events when arriving at stops to system error. For developers only.
   */
  public DebugRaptor raptorDebugging = new DebugRaptor();

  private Envelope fromEnvelope;

  private Envelope toEnvelope;

  /* CONSTRUCTORS */

  /** Constructor for options; modes defaults to walk and transit */
  public RouteRequest() {
    this.preferences = new RoutingPreferences();
    // So that they are never null.
    from = new GenericLocation(null, null);
    to = new GenericLocation(null, null);
  }

  public RouteRequest(TraverseModeSet streetSubRequestModes) {
    this();
    this.setStreetSubRequestModes(streetSubRequestModes);
  }

  public RouteRequest(TraverseMode mode) {
    this();
    this.setStreetSubRequestModes(new TraverseModeSet(mode));
  }

  /* ACCESSOR/SETTER METHODS */
  public RouteRequest(RequestModes modes) {
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

  public RoutingPreferences preferences() {
    return preferences;
  }

  /**
   * Whether the trip must be wheelchair-accessible
   */
  public boolean wheelchair() {
    return wheelchair;
  }

  public void setWheelchair(boolean wheelchair) {
    this.wheelchair = wheelchair;
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

  public void setPreferredRoutes(Collection<FeedScopedId> routeIds) {
    preferredRoutes = routeIds.stream().toList();
  }

  public void setPreferredRoutesFromString(String s) {
    if (!s.isEmpty()) {
      preferredRoutes = FeedScopedId.parseListOfIds(s);
    } else {
      preferredRoutes = List.of();
    }
  }

  public void setUnpreferredRoutes(Collection<FeedScopedId> routeIds) {
    unpreferredRoutes = Set.copyOf(routeIds);
  }

  public void setUnpreferredRoutesFromString(String s) {
    if (!s.isEmpty()) {
      unpreferredRoutes = Set.copyOf(FeedScopedId.parseListOfIds(s));
    } else {
      unpreferredRoutes = Set.of();
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
   * The epoch date/time in seconds that the trip should depart (or arrive, for requests where
   * arriveBy is true)
   * <p>
   * The search time for the current request. If the client have moved to the next page then this is
   * the adjusted search time - the dateTime passed in is ignored and replaced with by a time from
   * the pageToken.
   */
  public Instant dateTime() {
    return dateTime;
  }

  public void setDateTime(Instant dateTime) {
    this.dateTime = dateTime;
  }

  public void setDateTime(String date, String time, ZoneId tz) {
    ZonedDateTime dateObject = DateUtils.toZonedDateTime(date, time, tz);
    setDateTime(dateObject == null ? Instant.now() : dateObject.toInstant());
  }

  /**
   * Is the trip originally planned withing the previous/next 15h?
   */
  public boolean isTripPlannedForNow() {
    return Duration.between(dateTime, Instant.now()).abs().toSeconds() < NOW_THRESHOLD_SEC;
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
      modes = modes.copy().withDirectMode(StreetMode.NOT_SET).build();
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

  public String toString(String sep) {
    return (
      from + sep + to + sep + dateTime + sep + arriveBy + sep + streetSubRequestModes.getAsStr()
    );
  }

  /* INSTANCE METHODS */

  public RouteRequest getStreetSearchRequest(StreetMode streetMode) {
    RouteRequest streetRequest = this.clone();
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

  /**
   * This method is used to clone the default message, and insert a current time. A typical use-case
   * is to copy the default request(from router-config), and then set all user specified parameters
   * before performing a routing search.
   */
  public RouteRequest copyWithDateTimeNow() {
    RouteRequest copy = clone();
    copy.setDateTime(Instant.now());
    return copy;
  }

  @Override
  public RouteRequest clone() {
    try {
      RouteRequest clone = (RouteRequest) super.clone();
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
      clone.preferredRoutes = List.copyOf(preferredRoutes);
      clone.unpreferredRoutes = Set.copyOf(unpreferredRoutes);

      clone.bannedTrips = Set.copyOf(bannedTrips);

      clone.allowedRentalFormFactors = new HashSet<>(allowedRentalFormFactors);

      clone.raptorDebugging = new DebugRaptor(this.raptorDebugging);
      clone.preferences = preferences.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  public String toString() {
    return toString(" ");
  }

  public RouteRequest reversedClone() {
    RouteRequest ret = this.clone();
    ret.setArriveBy(!ret.arriveBy);
    preferences().rental().setUseAvailabilityInformation(false);
    return ret;
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

  public Set<FeedScopedId> getUnpreferredAgencies() {
    return unpreferredAgencies;
  }

  public Set<FeedScopedId> getUnpreferredRoutes() {
    return unpreferredRoutes;
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
   * @see RouteRequest#MAX_CLOSENESS_METERS
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

  /** The start location */
  public GenericLocation from() {
    return from;
  }

  public void setFrom(GenericLocation from) {
    this.from = from;
  }

  /** The end location */
  public GenericLocation to() {
    return to;
  }

  public void setTo(GenericLocation to) {
    this.to = to;
  }

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
  public Duration searchWindow() {
    return searchWindow;
  }

  public void setSearchWindow(Duration searchWindow) {
    this.searchWindow = searchWindow;
  }

  public Locale locale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

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
  public PageCursor pageCursor() {
    return pageCursor;
  }

  public void setPageCursorFromEncoded(String pageCursor) {
    this.pageCursor = PageCursor.decode(pageCursor);
  }

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
  public boolean timetableView() {
    return timetableView;
  }

  public void setTimetableView(boolean timetableView) {
    this.timetableView = timetableView;
  }

  /**
   * Whether the trip should depart at dateTime (false, the default), or arrive at dateTime.
   */
  public boolean arriveBy() {
    return arriveBy;
  }

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
  public int numItineraries() {
    return numItineraries;
  }

  public void setNumItineraries(int numItineraries) {
    this.numItineraries = numItineraries;
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
}
