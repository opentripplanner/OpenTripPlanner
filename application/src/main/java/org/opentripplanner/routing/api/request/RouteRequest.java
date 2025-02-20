package org.opentripplanner.routing.api.request;

import static org.opentripplanner.utils.time.DurationUtils.durationInSeconds;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.request.JourneyRequest;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig;
import org.opentripplanner.utils.collection.ListSection;
import org.opentripplanner.utils.lang.ObjectUtils;
import org.opentripplanner.utils.time.DateUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A trip planning request. Some parameters may not be honored by the trip planner for some or all
 * itineraries.
 * <p>
 * All defaults should be specified here in the RouteRequest, NOT as annotations on query parameters
 * in web services that create RouteRequests. This establishes a priority chain for default values:
 * RouteRequest field initializers, then JSON router config, then query parameters.
 */
public class RouteRequest implements Cloneable, Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(RouteRequest.class);

  private static final long NOW_THRESHOLD_SEC = durationInSeconds("15h");

  /* FIELDS UNIQUELY IDENTIFYING AN SPT REQUEST */

  private GenericLocation from;

  private GenericLocation to;

  private List<ViaLocation> via = Collections.emptyList();

  private Instant dateTime = Instant.now();

  @Nullable
  private Duration maxSearchWindow;

  private Duration searchWindow;

  private PageCursor pageCursor;

  private boolean timetableView = true;

  private boolean arriveBy = false;

  private int numItineraries = 50;

  private Locale locale = new Locale("en", "US");

  private RoutingPreferences preferences = new RoutingPreferences();

  private JourneyRequest journey = new JourneyRequest();

  private boolean wheelchair = false;

  private Instant bookingTime;

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

  public RouteRequest withPreferences(Consumer<RoutingPreferences.Builder> body) {
    this.preferences = preferences.copyOf().apply(body).build();
    return this;
  }

  /**
   * The booking time is used to exclude services which are not bookable at the
   * requested booking time. If a service is bookable at this time or later, the service
   * is included. This applies to FLEX access, egress and direct services.
   */
  public Instant bookingTime() {
    return bookingTime;
  }

  public RouteRequest setBookingTime(Instant bookingTime) {
    this.bookingTime = bookingTime;
    return this;
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
      return pageCursor.originalSortOrder();
    }
    return arriveBy ? SortOrder.STREET_AND_DEPARTURE_TIME : SortOrder.STREET_AND_ARRIVAL_TIME;
  }

  /**
   * Adjust the 'dateTime' if the page cursor is set to "goto next/previous page". The date-time is
   * used for many things, for example finding the days to search, but the transit search is using
   * the cursor[if exist], not the date-time.
   * <p>
   * The direct mode is also unset when there is a page cursor because for anything other than the
   * initial page we don't want to see direct results.
   * <p>
   * See also {@link org.opentripplanner.routing.algorithm.raptoradapter.router.FilterTransitWhenDirectModeIsEmpty},
   * it uses a direct search to prune transit.
   */
  public void applyPageCursor() {
    if (pageCursor != null) {
      // We switch to "depart-after" search when paging next(lat==null). It does not make
      // sense anymore to keep the latest-arrival-time when going to the "next page".
      if (pageCursor.latestArrivalTime() == null) {
        arriveBy = false;
      }
      this.dateTime =
        arriveBy ? pageCursor.latestArrivalTime() : pageCursor.earliestDepartureTime();
      journey.setModes(journey.modes().copyOf().withDirectMode(StreetMode.NOT_SET).build());
      LOG.debug("Request dateTime={} set from pageCursor.", dateTime);
    }
  }

  /**
   * When paging we must crop the list of itineraries in the right end according to the sorting of
   * the original search and according to the paging direction (next or previous). We always
   * crop at the end of the initial search. This is a utility function delegating to the
   * pageCursor, if available.
   */
  public ListSection cropItinerariesAt() {
    return pageCursor == null ? ListSection.TAIL : pageCursor.cropItinerariesAt();
  }

  /**
   * Validate that the routing request contains both an origin and a destination. Origin and
   * destination can be specified either by a reference to a stop place or by geographical
   * coordinates. Origin and destination are required in a one-to-one search, but not in a
   * many-to-one or one-to-many.
   * TODO - Refactor and make separate requests for one-to-one and the other searches.
   *
   * @throws RoutingValidationException if either origin or destination is missing.
   */
  public void validateOriginAndDestination() {
    List<RoutingError> routingErrors = new ArrayList<>(2);

    if (from == null || !from.isSpecified()) {
      routingErrors.add(
        new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.FROM_PLACE)
      );
    }

    if (to == null || !to.isSpecified()) {
      routingErrors.add(new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.TO_PLACE));
    }

    if (!routingErrors.isEmpty()) {
      throw new RoutingValidationException(routingErrors);
    }
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
   * TransferOptimization is appled to all results exept via-visit requests.
   * TODO VIA - When the Optimized transfer support this, then this method should be removed.
   */
  public boolean allowTransferOptimization() {
    return !isViaSearch() || via.stream().allMatch(ViaLocation::isPassThroughLocation);
  }

  /**
   * Return {@code true} if at least one via location is set!
   */
  public boolean isViaSearch() {
    return !via.isEmpty();
  }

  public List<ViaLocation> getViaLocations() {
    return via;
  }

  public RouteRequest setViaLocations(final List<ViaLocation> via) {
    this.via = via;
    return this;
  }

  /**
   * This is the time/duration in seconds from the earliest-departure-time(EDT) to
   * latest-departure-time(LDT). In case of a reverse search, it will be the time from earliest to
   * latest arrival time (LAT - EAT).
   * <p>
   * All optimal itineraries that depart within the search window are guaranteed to be found.
   * <p>
   * This is sometimes referred to as the Range Raptor Search Window - but could be used in a none
   * Transit search as well; Hence this is named search-window and not raptor-search-window. Do not
   * confuse this with the travel-window, which is the time between EDT to LAT.
   * <p>
   * Use {@code null} to unset, and {@link Duration#ZERO} to do one Raptor iteration. The value is
   * dynamically assigned a suitable value, if not set. In a small-to-medium size operation, you may
   * use a fixed value, like 60 minutes. If you have a mixture of high-frequency city routes and
   * infrequent long distant journeys, the best option is normally to use the dynamic auto
   * assignment.
   * <p>
   * There is no need to set this when going to the next/previous page anymore.
   */
  public Duration searchWindow() {
    return searchWindow;
  }

  public void setSearchWindow(@Nullable Duration searchWindow) {
    if (searchWindow != null) {
      if (hasMaxSearchWindow() && searchWindow.toSeconds() > maxSearchWindow.toSeconds()) {
        throw new IllegalArgumentException("The search window cannot exceed " + maxSearchWindow);
      }
      if (searchWindow.isNegative()) {
        throw new IllegalArgumentException("The search window must be a positive duration");
      }
    }
    this.searchWindow = searchWindow;
  }

  private boolean hasMaxSearchWindow() {
    return maxSearchWindow != null;
  }

  /**
   * For testing only. Use {@link TransitRoutingConfig#maxSearchWindow()} instead.
   * @see #initMaxSearchWindow(Duration)
   */
  public Duration maxSearchWindow() {
    return maxSearchWindow;
  }

  /**
   * Initialize the maxSearchWindow from the transit config. This is necessary because the
   * default route request is configured before the {@link TransitRoutingConfig}.
   */
  public void initMaxSearchWindow(Duration maxSearchWindow) {
    this.maxSearchWindow =
      ObjectUtils.requireNotInitialized(
        "maxSearchWindow",
        this.maxSearchWindow,
        Objects.requireNonNull(maxSearchWindow)
      );
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

  public String toString() {
    return ToStringBuilder
      .of(RouteRequest.class)
      .addObj("from", from)
      .addObj("to", to)
      .addDateTime("dateTime", dateTime)
      .addBoolIfTrue("arriveBy", arriveBy)
      .addObj("modes", journey.modes())
      .addCol("filters", journey.transit().filters())
      .toString();
  }
}
