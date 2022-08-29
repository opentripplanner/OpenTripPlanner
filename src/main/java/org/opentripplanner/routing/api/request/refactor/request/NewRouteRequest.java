package org.opentripplanner.routing.api.request.refactor.request;

import static org.opentripplanner.util.time.DurationUtils.durationInSeconds;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.common.LocationStringParser;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.pagecursor.PageCursor;
import org.opentripplanner.model.plan.pagecursor.PageType;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.refactor.preference.RoutingPreferences;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.util.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: 2022-08-18 rename class when done
public class NewRouteRequest implements Cloneable, Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(NewRouteRequest.class);

  /* FIELDS UNIQUELY IDENTIFYING AN SPT REQUEST */
  /**
   * How close to do you have to be to the start or end to be considered "close".
   *
   * @see RoutingRequest#isCloseToStartOrEnd(Vertex)
   * @see DominanceFunction#betterOrEqualAndComparable(State, State)
   */
  private static final int MAX_CLOSENESS_METERS = 500;

  private static final long NOW_THRESHOLD_SEC = durationInSeconds("15h");

  /**
   * The epoch date/time in seconds that the trip should depart (or arrive, for requests where
   * arriveBy is true)
   */
  private Instant dateTime;
  /** The start location */
  private GenericLocation from;

  private Envelope fromEnvelope;

  /** The end location */
  private GenericLocation to;

  private Envelope toEnvelope;

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
  private Duration searchWindow;
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
  private PageCursor pageCursor;
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
  private boolean timetableView;
  /**
   * Whether the trip should depart at dateTime (false, the default), or arrive at dateTime.
   */
  private boolean arriveBy = false;
  private Locale locale = new Locale("en", "US");
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
  private int numItineraries = 50;

  private JourneyRequest journey = new JourneyRequest();

  /**
   * A trip where this trip must start from (depart-onboard routing)
   *
   * @deprecated TODO OTP2 Regression. Not currently working in OTP2. We might not implement the
   * old functionality the same way, but we will try to map this parameter
   * so it does work similar as before.
   */
  @Deprecated
  private FeedScopedId startingTransitTripId;

  public NewRouteRequest() {
    // So that they are never null.
    from = new GenericLocation(null, null);
    to = new GenericLocation(null, null);
  }

  public NewRouteRequest(TraverseMode mode) {
    this();
    this.journey.setStreetSubRequestModes(new TraverseModeSet(mode));
  }

  public NewRouteRequest(TraverseModeSet modeSet) {
    this();
    this.journey.setStreetSubRequestModes(modeSet);
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

      this.journey = journey.clone();
      journey.direct().setMode(StreetMode.NOT_SET);
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

  public SortOrder itinerariesSortOrder() {
    if (pageCursor != null) {
      return pageCursor.originalSortOrder;
    }
    return arriveBy ? SortOrder.STREET_AND_DEPARTURE_TIME : SortOrder.STREET_AND_ARRIVAL_TIME;
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

  // TODO: 2022-08-18 This probably should not be here
  public Pair<NewRouteRequest, RoutingPreferences> getStreetSearchRequestAndPreferences(
    StreetMode streetMode,
    RoutingPreferences routingPreferences
  ) {
    var streetRequest = this.clone();
    var streetPreferences = routingPreferences.clone();
    var journeyRequest = streetRequest.journey;
    journeyRequest.setStreetSubRequestModes(new TraverseModeSet());

    if (streetMode != null) {
      switch (streetMode) {
        case WALK, FLEXIBLE -> journeyRequest.setStreetSubRequestModes(
          new TraverseModeSet(TraverseMode.WALK)
        );
        case BIKE -> journeyRequest.setStreetSubRequestModes(
          new TraverseModeSet(TraverseMode.BICYCLE)
        );
        case BIKE_TO_PARK -> {
          journeyRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK)
          );
          routingPreferences.bike().setParkAndRide(true);
        }
        case BIKE_RENTAL -> {
          journeyRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK)
          );
          routingPreferences.rental().setAllow(true);
          journeyRequest.rental().allowedFormFactors().add(RentalVehicleType.FormFactor.BICYCLE);
        }
        case SCOOTER_RENTAL -> {
          journeyRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK)
          );
          routingPreferences.rental().setAllow(true);
          journeyRequest.rental().allowedFormFactors().add(RentalVehicleType.FormFactor.SCOOTER);
        }
        case CAR -> journeyRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.CAR));
        case CAR_TO_PARK -> {
          journeyRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK)
          );
          routingPreferences.car().setParkAndRide(true);
        }
        case CAR_PICKUP -> {
          journeyRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK)
          );
          routingPreferences.car().allowPickup();
        }
        case CAR_RENTAL -> {
          journeyRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK)
          );
          routingPreferences.rental().setAllow(true);
          journeyRequest.rental().allowedFormFactors().add(RentalVehicleType.FormFactor.CAR);
        }
      }
    }

    return Pair.of(streetRequest, streetPreferences);
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
   * @see NewRouteRequest#MAX_CLOSENESS_METERS
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

  public NewRouteRequest reversedClone() {
    var request = this.clone();
    request.setArriveBy(!request.arriveBy);

    return request;
  }

  private void setJourney(JourneyRequest journey) {
    this.journey = journey;
  }

  public NewRouteRequest clone() {
    try {
      // TODO: 2022-08-25 there are some fields which will not be cloned in proper way
      // but that's how it was implemented before so I'm leaving it like that

      var clone = (NewRouteRequest) super.clone();
      clone.setJourney(this.journey().clone());

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  public NewRouteRequest copyWithDateTimeNow() {
    var request = clone();
    request.setDateTime(Instant.now());
    return request;
  }

  /**
   * Is the trip originally planned withing the previous/next 15h?
   */
  public boolean isTripPlannedForNow() {
    return Duration.between(dateTime, Instant.now()).abs().toSeconds() < NOW_THRESHOLD_SEC;
  }

  public void setDateTime(Instant dateTime) {
    this.dateTime = dateTime;
  }

  public void setDateTime(String date, String time, ZoneId tz) {
    ZonedDateTime dateObject = DateUtils.toZonedDateTime(date, time, tz);
    setDateTime(dateObject == null ? Instant.now() : dateObject.toInstant());
  }

  public Instant dateTime() {
    return dateTime;
  }

  public void setFromString(String from) {
    this.from = LocationStringParser.fromOldStyleString(from);
  }

  public void setFrom(GenericLocation from) {
    this.from = from;
  }

  public GenericLocation from() {
    return from;
  }

  public void setToString(String to) {
    this.to = LocationStringParser.fromOldStyleString(to);
  }

  public void setTo(GenericLocation to) {
    this.to = to;
  }

  public GenericLocation to() {
    return to;
  }

  public void setSearchWindow(Duration searchWindow) {
    this.searchWindow = searchWindow;
  }

  public Duration searchWindow() {
    return searchWindow;
  }

  public void setPageCursor(PageCursor pageCursor) {
    this.pageCursor = pageCursor;
  }

  public PageCursor pageCursor() {
    return pageCursor;
  }

  public void setTimetableView(boolean timetableView) {
    this.timetableView = timetableView;
  }

  public boolean timetableView() {
    return timetableView;
  }

  public void setArriveBy(boolean arriveBy) {
    this.arriveBy = arriveBy;
  }

  public boolean arriveBy() {
    return arriveBy;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
  }

  public Locale locale() {
    return locale;
  }

  public void setNumItineraries(int numItineraries) {
    this.numItineraries = numItineraries;
  }

  public int numItineraries() {
    return numItineraries;
  }

  public JourneyRequest journey() {
    return journey;
  }

  public void setStartingTransitTripId(FeedScopedId startingTransitTripId) {
    this.startingTransitTripId = startingTransitTripId;
  }

  public FeedScopedId startingTransitTripId() {
    return startingTransitTripId;
  }
}
