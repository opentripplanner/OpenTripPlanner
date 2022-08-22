package org.opentripplanner.routing.api.request.refactor.request;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.model.plan.pagecursor.PageCursor;
import org.opentripplanner.model.plan.pagecursor.PageType;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.refactor.preference.RoutingPreferences;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: 2022-08-18 rename class when done
public class NewRouteRequest {

  private static final Logger LOG = LoggerFactory.getLogger(NewRouteRequest.class);

  /**
   * The epoch date/time in seconds that the trip should depart (or arrive, for requests where
   * arriveBy is true)
   */
  protected Instant dateTime;
  /** The start location */
  protected GenericLocation from;
  /** The end location */
  protected GenericLocation to;
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
  protected Duration searchWindow;
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
  protected PageCursor pageCursor;
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
  protected boolean timetableView;
  /**
   * Whether the trip should depart at dateTime (false, the default), or arrive at dateTime.
   */
  protected boolean arriveBy = false;
  protected Locale locale = new Locale("en", "US");
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
  protected int numItineraries = 50;

  protected JourneyRequest journeyRequest = new JourneyRequest();

  // TODO: 2022-08-18 Should it be here?
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
  private Duration maxJourneyDuration = Duration.ofHours(24);

  public NewRouteRequest() {
    // So that they are never null.
    from = new GenericLocation(null, null);
    to = new GenericLocation(null, null);
  }

  public NewRouteRequest(TraverseMode mode) {
    this();
    this.journeyRequest.setStreetSubRequestModes(new TraverseModeSet(mode));
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

      // TODO: 2022-08-18 this was a builder pattern before
      journeyRequest.direct().setMode(StreetMode.NOT_SET);
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

  // TODO: 2022-08-18 This should probably not be here
  public NewRouteRequest getStreetSearchRequest(
    StreetMode streetMode,
    RoutingPreferences routingPreferences
  ) {
    NewRouteRequest streetRequest = this.clone();
    var journeyRequest = streetRequest.journeyRequest;
    journeyRequest.setStreetSubRequestModes(new TraverseModeSet());

    if (streetMode != null) {
      switch (streetMode) {
        case WALK:
        case FLEXIBLE:
          journeyRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.WALK));
          break;
        case BIKE:
          journeyRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.BICYCLE));
          break;
        case BIKE_TO_PARK:
          journeyRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK)
          );
          routingPreferences.bike().setParkAndRide(true);
          break;
        case BIKE_RENTAL:
          journeyRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK)
          );
          routingPreferences.rental().setAllow(true);
          // TODO: 2022-08-18 does it make sense?
          journeyRequest
            .direct()
            .vehicleRental()
            .allowedFormFactors()
            .add(RentalVehicleType.FormFactor.BICYCLE);
          journeyRequest
            .egress()
            .vehicleRental()
            .allowedFormFactors()
            .add(RentalVehicleType.FormFactor.BICYCLE);
          journeyRequest
            .access()
            .vehicleRental()
            .allowedFormFactors()
            .add(RentalVehicleType.FormFactor.BICYCLE);
          break;
        case SCOOTER_RENTAL:
          journeyRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.BICYCLE, TraverseMode.WALK)
          );
          routingPreferences.rental().setAllow(true);
          // TODO: 2022-08-18 does it make sense?
          journeyRequest
            .direct()
            .vehicleRental()
            .allowedFormFactors()
            .add(RentalVehicleType.FormFactor.SCOOTER);
          journeyRequest
            .egress()
            .vehicleRental()
            .allowedFormFactors()
            .add(RentalVehicleType.FormFactor.SCOOTER);
          journeyRequest
            .access()
            .vehicleRental()
            .allowedFormFactors()
            .add(RentalVehicleType.FormFactor.SCOOTER);
          break;
        case CAR:
          journeyRequest.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.CAR));
          break;
        case CAR_TO_PARK:
          journeyRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK)
          );
          routingPreferences.car().setParkAndRide(true);
          break;
        case CAR_PICKUP:
          journeyRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK)
          );
          routingPreferences.car().allowPickup();
          break;
        case CAR_RENTAL:
          journeyRequest.setStreetSubRequestModes(
            new TraverseModeSet(TraverseMode.CAR, TraverseMode.WALK)
          );
          routingPreferences.rental().setAllow(true);
          journeyRequest
            .direct()
            .vehicleRental()
            .allowedFormFactors()
            .add(RentalVehicleType.FormFactor.CAR);
          journeyRequest
            .egress()
            .vehicleRental()
            .allowedFormFactors()
            .add(RentalVehicleType.FormFactor.CAR);
          journeyRequest
            .access()
            .vehicleRental()
            .allowedFormFactors()
            .add(RentalVehicleType.FormFactor.CAR);
      }
    }

    return streetRequest;
  }

  public NewRouteRequest reversedClone() {
    // TODO: 2022-08-22 Implement it
    throw new RuntimeException("Not implemented");
  }

  // TODO: 2022-08-18 implement
  protected NewRouteRequest clone() {
    return this;
  }

  public void setDateTime(Instant dateTime) {
    this.dateTime = dateTime;
  }

  public Instant dateTime() {
    return dateTime;
  }

  public void setFrom(GenericLocation from) {
    this.from = from;
  }

  public GenericLocation from() {
    return from;
  }

  public void setTo(GenericLocation to) {
    this.to = to;
  }

  public GenericLocation to() {
    return to;
  }

  public Duration searchWindow() {
    return searchWindow;
  }

  public PageCursor pageCursor() {
    return pageCursor;
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

  public Locale locale() {
    return locale;
  }

  public int numItineraries() {
    return numItineraries;
  }

  public JourneyRequest journeyRequest() {
    return journeyRequest;
  }

  public Duration maxJourneyDuration() {
    return maxJourneyDuration;
  }
}
