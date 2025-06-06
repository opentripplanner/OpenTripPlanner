package org.opentripplanner.routing.api.request;

import static org.opentripplanner.utils.time.DurationUtils.durationInSeconds;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A trip planning request. Some parameters may not be honored by the trip planner for some or all
 * itineraries.
 * <p>
 * All defaults should be specified here in the RouteRequest, NOT as default values of parameters
 * in web services that create RouteRequests. This establishes a priority chain for default values:
 * RouteRequest field initializers, then JSON router config, then query parameters.
 */
public class RouteRequest implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(RouteRequest.class);

  private static final int DEFAULT_NUM_ITINERARIES = 50;
  private static final long NOW_THRESHOLD_SEC = durationInSeconds("15h");

  private static final RouteRequest DEFAULT = new RouteRequest();

  private final GenericLocation from;
  private final GenericLocation to;
  private final List<ViaLocation> via;
  private final Instant dateTime;
  private final boolean arriveBy;
  private final boolean timetableView;

  @Nullable
  private final Duration searchWindow;

  @Nullable
  private final Duration maxSearchWindow;

  @Nullable
  private final Instant bookingTime;

  @Nullable
  private final PageCursor pageCursor;

  private final JourneyRequest journey;
  private final RoutingPreferences preferences;
  private final int numItineraries;
  private final boolean defaultRequest;

  /* CONSTRUCTORS */

  private RouteRequest() {
    // So that they are never null.
    this.from = null;
    this.to = null;
    this.via = Collections.emptyList();
    this.dateTime = null;
    this.arriveBy = false;
    this.timetableView = true;
    this.searchWindow = null;
    this.maxSearchWindow = null;
    this.bookingTime = null;
    this.pageCursor = null;
    this.journey = JourneyRequest.DEFAULT;
    this.preferences = RoutingPreferences.DEFAULT;
    this.numItineraries = DEFAULT_NUM_ITINERARIES;
    this.defaultRequest = true;
  }

  RouteRequest(RouteRequestBuilder builder) {
    this.from = builder.from;
    this.to = builder.to;
    this.via = builder.via;

    this.dateTime = (!builder.defaultRequest && builder.dateTime == null)
      ? normalizeNow()
      : normalizeDateTime(builder.dateTime);

    this.arriveBy = builder.arriveBy;
    this.timetableView = builder.timetableView;
    this.searchWindow = builder.searchWindow;
    this.maxSearchWindow = builder.maxSearchWindow;
    this.bookingTime = builder.bookingTime;
    this.pageCursor = builder.pageCursor;
    this.journey = builder.journey;
    this.preferences = builder.preferences;
    this.numItineraries = builder.numItineraries;
    this.defaultRequest = builder.defaultRequest;

    validate();
  }

  public static RouteRequestBuilder of() {
    return DEFAULT.copyOf();
  }

  public static RouteRequest defaultValue() {
    return DEFAULT;
  }

  /**
   * The given {@code dateTime} will be set to a whole number of seconds. We don't do sub-second
   * accuracy, and if we set the millisecond part to a non-zero value, rounding will not be
   * guaranteed to be the same for departAt and arriveBy queries.
   *
   * If the given {@code dateTime} is {@code null}, then {@code null} is returned
   */
  @Nullable
  public static Instant normalizeDateTime(@Nullable Instant dateTime) {
    return dateTime == null ? null : dateTime.truncatedTo(ChronoUnit.SECONDS);
  }

  public static Instant normalizeNow() {
    return normalizeDateTime(Instant.now());
  }

  public RouteRequestBuilder copyOf() {
    return new RouteRequestBuilder(this);
  }

  /* ACCESSOR/SETTER METHODS */

  public JourneyRequest journey() {
    return journey;
  }

  public RoutingPreferences preferences() {
    return preferences;
  }

  /**
   * The booking time is used to exclude services which are not bookable at the
   * requested booking time. If a service is bookable at this time or later, the service
   * is included. This applies to FLEX access, egress and direct services.
   */
  public Instant bookingTime() {
    return bookingTime;
  }

  /**
   * The epoch date/time in seconds that the trip should depart (or arrive, for requests where
   * arriveBy is true)
   * <p>
   * The search time for the current request. If the client have moved to the next page then this is
   * the adjusted search time - the dateTime passed in is ignored and replaced with by a time from
   * the pageToken.
   *
   * This method returns {@code null} for default requests.
   */
  @Nullable
  public Instant dateTime() {
    return dateTime;
  }

  /**
   * Is the trip originally planned withing the previous/next 15h?
   */
  public static boolean isAPIGtfsTripPlannedForNow(Instant dateTime) {
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
   * initial page we don't want to see direct results. If the direct mode was given in the first request,
   * the generalized cost of the direct mode itinerary from the initial request is stored in the page cursor
   * for use with {@link org.opentripplanner.routing.algorithm.filterchain.filters.transit.RemoveTransitIfStreetOnlyIsBetter}
   * to filter away unwanted transit results.
   */
  public RouteRequest withPageCursor() {
    if (pageCursor == null) {
      return this;
    }
    boolean arriveBy = this.arriveBy;

    // We switch to "depart-after" search when paging next(lat==null). It does not make
    // sense anymore to keep the latest-arrival-time when going to the "next page".
    if (pageCursor.latestArrivalTime() == null) {
      arriveBy = false;
    }

    var request = copyOf()
      .withArriveBy(arriveBy)
      .withDateTime(arriveBy ? pageCursor.latestArrivalTime() : pageCursor.earliestDepartureTime())
      .withJourney(jb -> jb.withoutDirect())
      .buildRequest();

    LOG.debug("Request dateTime={} set from pageCursor.", request.dateTime());
    return request;
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

  /**
   * The origin/start location.
   *
   * Returns {@code null} for default requests.
   */
  @Nullable
  public GenericLocation from() {
    return from;
  }

  /**
   * The destination/end location.
   *
   * Returns {@code null} for default requests.
   */
  @Nullable
  public GenericLocation to() {
    return to;
  }

  /**
   * TransferOptimization is applied to all results except via-visit requests.
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

  boolean isDefaultRequest() {
    return defaultRequest;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var other = (RouteRequest) o;

    // The 'defaultRequest' field does not need to be part of equals and hashCode
    // because it is not possible to create the same object where only this field
    // is diffrent.

    return (
      arriveBy == other.arriveBy &&
      timetableView == other.timetableView &&
      numItineraries == other.numItineraries &&
      Objects.equals(from, other.from) &&
      Objects.equals(to, other.to) &&
      Objects.equals(via, other.via) &&
      Objects.equals(dateTime, other.dateTime) &&
      Objects.equals(searchWindow, other.searchWindow) &&
      Objects.equals(maxSearchWindow, other.maxSearchWindow) &&
      Objects.equals(bookingTime, other.bookingTime) &&
      Objects.equals(pageCursor, other.pageCursor) &&
      Objects.equals(journey, other.journey) &&
      Objects.equals(preferences, other.preferences)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      from,
      to,
      via,
      dateTime,
      arriveBy,
      timetableView,
      searchWindow,
      maxSearchWindow,
      bookingTime,
      pageCursor,
      journey,
      preferences,
      numItineraries
    );
  }

  public String toString() {
    return ToStringBuilder.of(RouteRequest.class)
      .addObj("from", from)
      .addObj("to", to)
      .addCol("via", via)
      .addDateTime("dateTime", dateTime)
      .addBoolIfTrue("arriveBy", arriveBy)
      .addBoolIfTrue("timetableView: false", !timetableView)
      .addDuration("searchWindow", searchWindow)
      .addDuration("maxSearchWindow", maxSearchWindow)
      .addDateTime("bookingTime", bookingTime)
      .addNum("numItineraries", numItineraries, DEFAULT_NUM_ITINERARIES)
      .addObj("preferences", preferences, RoutingPreferences.DEFAULT)
      .addObj("journey", journey, JourneyRequest.DEFAULT)
      .toString();
  }

  private void validate() {
    validateTimeNotSetIfDefaultRequest();

    // TODO: Use the same strateg for all validation of the RouteRequest, currently
    //       both RoutingError and IllegalArgumentException is used.
    List<RoutingError> routingErrors = new ArrayList<>(2);
    routingErrors.addAll(validateFromAndToLocation());

    validateSearchWindow();

    if (!routingErrors.isEmpty()) {
      throw new RoutingValidationException(routingErrors);
    }
  }

  private void validateTimeNotSetIfDefaultRequest() {
    if (defaultRequest && dateTime != null) {
      throw new IllegalStateException();
    }
  }

  /**
   * Validate that the routing request contains both a from location(origin) and a to
   * location(destination). Origin and destination can be specified either by a reference to a stop
   * place or by geographical coordinates. From/to locations are required in a one-to-one
   * search, but not in a many-to-one or one-to-many(legacy, not supported any more).
   *
   * @throws RoutingValidationException if either origin or destination is missing.
   */
  public List<RoutingError> validateFromAndToLocation() {
    if (defaultRequest) {
      if (from != null || to != null) {
        throw new IllegalStateException("from=" + from + ", to=" + to);
      }
      return List.of();
    }

    List<RoutingError> routingErrors = new ArrayList<>(2);

    if (from == null || !from.isSpecified()) {
      routingErrors.add(
        new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.FROM_PLACE)
      );
    }

    if (to == null || !to.isSpecified()) {
      routingErrors.add(new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.TO_PLACE));
    }
    return routingErrors;
  }

  private void validateSearchWindow() {
    if (searchWindow != null) {
      if (hasMaxSearchWindow() && searchWindow.toSeconds() > maxSearchWindow.toSeconds()) {
        throw new IllegalArgumentException("The search window cannot exceed " + maxSearchWindow);
      }
      if (searchWindow.isNegative()) {
        throw new IllegalArgumentException("The search window must be a positive duration");
      }
    }
  }
}
