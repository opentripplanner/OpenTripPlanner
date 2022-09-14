package org.opentripplanner.routing.api.request;

import static org.opentripplanner.util.time.DurationUtils.durationInSeconds;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.pagecursor.PageCursor;
import org.opentripplanner.model.plan.pagecursor.PageType;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;
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

  /**
   * How close to do you have to be to the start or end to be considered "close".
   *
   * @see RouteRequest#isCloseToStartOrEnd(Vertex)
   * @see DominanceFunction#betterOrEqualAndComparable(State, State)
   */
  private static final int MAX_CLOSENESS_METERS = 500;

  /* FIELDS UNIQUELY IDENTIFYING AN SPT REQUEST */

  private GenericLocation from;

  private GenericLocation to;

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

  private RoutingPreferences preferences = new RoutingPreferences();

  private JourneyRequest journey = new JourneyRequest();

  private boolean wheelchair = false;

  /*
      Additional flags affecting mode transitions.
      This is a temporary solution, as it only covers parking and rental at the beginning of the trip.
    */
  public boolean vehicleRental = false;
  public boolean parkAndRide = false;
  public boolean carPickup = false;
  public Set<FormFactor> allowedRentalFormFactors = new HashSet<>();

  private Envelope fromEnvelope;

  private Envelope toEnvelope;

  /* CONSTRUCTORS */

  /** Constructor for options; modes defaults to walk and transit */
  public RouteRequest() {
    // So that they are never null.
    from = new GenericLocation(null, null);
    to = new GenericLocation(null, null);
  }

  public RouteRequest(TraverseMode mode) {
    this();
    this.setStreetSubRequestModes(new TraverseModeSet(mode));
  }

  /* ACCESSOR/SETTER METHODS */

  public void setArriveBy(boolean arriveBy) {
    this.arriveBy = arriveBy;
  }

  public void setMode(TraverseMode mode) {
    setStreetSubRequestModes(new TraverseModeSet(mode));
  }

  public void setStreetSubRequestModes(TraverseModeSet streetSubRequestModes) {
    this.streetSubRequestModes = streetSubRequestModes;
  }

  public JourneyRequest journey() {
    return journey;
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

  public SortOrder itinerariesSortOrder() {
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
      this.dateTime = arriveBy ? pageCursor.latestArrivalTime : pageCursor.earliestDepartureTime;
      journey.setModes(journey.modes().copyOf().withDirectMode(StreetMode.NOT_SET).build());
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
      return itinerariesSortOrder().isSortedByArrivalTimeAcceding();
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

      clone.allowedRentalFormFactors = new HashSet<>(allowedRentalFormFactors);

      clone.preferences = preferences.clone();
      clone.journey = journey.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  public String toString() {
    return toString(" ");
  }

  public RouteRequest copyOfReversed() {
    RouteRequest ret = this.clone();
    ret.setArriveBy(!ret.arriveBy);
    return ret;
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
}
