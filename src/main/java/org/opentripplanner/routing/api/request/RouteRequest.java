package org.opentripplanner.routing.api.request;

import static org.opentripplanner.framework.time.DurationUtils.durationInSeconds;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.function.Consumer;
import org.opentripplanner.framework.time.DateUtils;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.pagecursor.PageCursor;
import org.opentripplanner.model.plan.pagecursor.PageType;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A trip planning request. Some parameters may not be honored by the trip planner for some or all
 * itineraries.
 * <p>
 * All defaults should be specified here in the RouteRequest, NOT as annotations on query parameters
 * in web services that create RouteRequests. This establishes a priority chain for default values:
 * RouteRequest field initializers, then JSON router config, then query parameters.
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

  private GenericLocation from;

  private GenericLocation to;

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

  /* CONSTRUCTORS */

  /** Constructor for options; modes defaults to walk and transit */
  public RouteRequest() {
    // So that they are never null.
    from = new GenericLocation(null, null);
    to = new GenericLocation(null, null);
  }

  /* ACCESSOR/SETTER METHODS */

  public void setJourney(JourneyRequest journey) {
    this.journey = journey;
  }

  public void setArriveBy(boolean arriveBy) {
    this.arriveBy = arriveBy;
  }

  public JourneyRequest journey() {
    return journey;
  }

  public RoutingPreferences preferences() {
    return preferences;
  }

  public void withPreferences(Consumer<RoutingPreferences.Builder> body) {
    this.preferences = preferences.copyOf().apply(body).build();
  }

  void setPreferences(RoutingPreferences preferences) {
    this.preferences = preferences;
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
    return from + sep + to + sep + dateTime + sep + arriveBy + sep + journey.modes();
  }

  /* INSTANCE METHODS */

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
}
