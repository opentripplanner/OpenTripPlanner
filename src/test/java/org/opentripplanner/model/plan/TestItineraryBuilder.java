package org.opentripplanner.model.plan;

import static java.time.ZoneOffset.UTC;
import static org.opentripplanner.routing.core.TraverseMode.BICYCLE;
import static org.opentripplanner.routing.core.TraverseMode.CAR;
import static org.opentripplanner.routing.core.TraverseMode.WALK;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.util.time.TimeUtils;

/**
 * This is a helper class to allow unit-testing on Itineraries. The builder does not necessarily
 * create complete/correct itineraries with legs - it is the unit-test which is responsible to
 * create an itinerary that have correct data which is consistent with what is produced by OTP. To
 * keep maintenance easy, create the minimum amount of data to focus your test - this also help
 * demonstrate which data is needed by the "code-under-test".
 * <p>
 * <b>Service Time in seconds</b>
 * Time in seconds past "midnight"(noon - 12h). The time is measured from "noon minus 12h" of the
 * service day (effectively midnight except for days on which daylight savings time changes occur).
 */
public class TestItineraryBuilder implements PlanTestConstants {
  public static final LocalDate SERVICE_DAY = LocalDate.of(2020, Month.FEBRUARY, 2);
  public static final Route BUS_ROUTE = route(TransitMode.BUS);
  public static final Route RAIL_ROUTE = route(TransitMode.RAIL);

  /**
   * For Transit Legs the stopIndex in from/to palce should be increasing. We do not use/lookup
   * the pattern or trip in the unit tests using this, so the index is fiction. Some of the
   * Itinerary filters rely on it to make sure to trips overlap.
   */
  private static final int TRIP_FROM_STOP_INDEX = 5;
  private static final int TRIP_TO_STOP_INDEX = 7;

  private Place lastPlace;
  private int lastEndTime;
  private int cost = 0;
  private final List<Leg> legs = new ArrayList<>();

  private TestItineraryBuilder(Place origin, int startTime) {
    this.lastPlace = origin;
    this.lastEndTime = startTime;
  }

  /**
   * Create a new itinerary that start at a stop and continue with a transit leg.
   */
  public static TestItineraryBuilder newItinerary(Place origin) {
    return new TestItineraryBuilder(origin, NOT_SET);
  }

  /**
   * Create a new itinerary that start by waling from a place - the origin.
   */
  public static TestItineraryBuilder newItinerary(Place origin, int startTime) {
    return new TestItineraryBuilder(origin, startTime);
  }

  /**
   * Add a walking leg to the itinerary
   * @param duration number of seconds to walk
   */
  public TestItineraryBuilder walk(int duration, Place to) {
    if(lastEndTime == NOT_SET) { throw new IllegalStateException("Start time unknown!"); }
    int legCost = cost(WALK_RELUCTANCE_FACTOR, duration);
    streetLeg(WALK, lastEndTime, lastEndTime + duration, to, legCost);
    return this;
  }

  /**
   * Add a bicycle leg to the itinerary.
   */
  public TestItineraryBuilder bicycle(int startTime, int endTime, Place to) {
    int legCost = cost(BICYCLE_RELUCTANCE_FACTOR, endTime - startTime);
    streetLeg(BICYCLE, startTime, endTime, to, legCost);
    return this;
  }

  public TestItineraryBuilder drive(int startTime, int endTime, Place to) {
    int legCost = cost(CAR_RELUCTANCE_FACTOR, endTime - startTime);
    streetLeg(CAR, startTime, endTime, to, legCost);
    return this;
  }

  /**
   * Add a rented bicycle leg to the itinerary.
   */
  public TestItineraryBuilder rentedBicycle(int startTime, int endTime, Place to) {
    int legCost = cost(BICYCLE_RELUCTANCE_FACTOR, endTime - startTime);
    streetLeg(BICYCLE, startTime, endTime, to, legCost);
    ((StreetLeg) this.legs.get(0)).setRentedVehicle(true);
    return this;
  }

  /**
   * Add a bus leg to the itinerary.
   */
  public TestItineraryBuilder bus(int tripId, int startTime, int endTime, int fromStopIndex, int toStopIndex, Place to, LocalDate serviceDate
  ) {
    return transit(BUS_ROUTE, tripId, startTime, endTime, fromStopIndex, toStopIndex, to, serviceDate);
  }

  public TestItineraryBuilder bus(int tripId, int startTime, int endTime, Place to) {
    return bus(tripId, startTime, endTime, TRIP_FROM_STOP_INDEX, TRIP_TO_STOP_INDEX, to, null);
  }

  /**
   * Add a rail/train leg to the itinerary
   */
  public TestItineraryBuilder rail(int tripId, int startTime, int endTime, Place to) {
    return transit(RAIL_ROUTE, tripId, startTime, endTime, TRIP_FROM_STOP_INDEX, TRIP_TO_STOP_INDEX, to, null);
  }

  public Itinerary egress(int walkDuration) {
    walk(walkDuration, null);
    return build();
  }

  public Itinerary build() {
    Itinerary itinerary = new Itinerary(legs);
    itinerary.generalizedCost = cost;
    return itinerary;
  }

  /**
   * The itinerary uses the old Java Calendar, but we would like to migrate to the new java.time
   * library; Hence this method is already changed. To convert into the legacy Calendar use
   * {@link GregorianCalendar#from(ZonedDateTime)} method.
   */
  public static ZonedDateTime newTime(int seconds) {
    return TimeUtils.zonedDateTime(SERVICE_DAY, seconds, UTC);
  }


  /* private methods */

  private TestItineraryBuilder transit(Route route, int tripId, int start, int end, int fromStopIndex, int toStopIndex, Place to, LocalDate serviceDate) {
    if(lastPlace == null) { throw new IllegalStateException("Trip from place is unknown!"); }
    int waitTime = start - lastEndTime(start);
    int legCost = 0;
    legCost += cost(WAIT_RELUCTANCE_FACTOR, waitTime);
    legCost += cost(1.0f, end - start) + BOARD_COST;

    Trip trip = trip(tripId, route);

    final List<StopTime> stopTimes = new ArrayList<>();
    StopTime fromStopTime = new StopTime();
    fromStopTime.setStop(lastPlace.stop);
    fromStopTime.setArrivalTime(start);
    fromStopTime.setDepartureTime(start);
    fromStopTime.setTrip(trip);

    // Duplicate stop time for all stops prior to the last.
    for (int i = 0 ; i < toStopIndex; i++) {
      stopTimes.add(fromStopTime);
    }

    StopTime toStopTime = new StopTime();
    toStopTime.setStop(to.stop);
    toStopTime.setArrivalTime(end);
    toStopTime.setDepartureTime(end);
    toStopTime.setTrip(trip);

    stopTimes.add(toStopTime);

    StopPattern stopPattern = new StopPattern(stopTimes);
    TripPattern tripPattern = new TripPattern(route.getId(), route, stopPattern);
    final TripTimes tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());
    tripPattern.add(tripTimes);

    ScheduledTransitLeg leg = new ScheduledTransitLeg(
            tripTimes,
            tripPattern,
            fromStopIndex,
            toStopIndex,
            GregorianCalendar.from(newTime(start)),
            GregorianCalendar.from(newTime(end)),
            serviceDate != null ? serviceDate : SERVICE_DAY,
            UTC,
            null,
            null,
            legCost
    );

    leg.setDistanceMeters(speed(leg.getMode()) * (end - start));

    legs.add(leg);
    cost += legCost;

    // Setup for adding another leg
    lastEndTime = end;
    lastPlace = to;

    return this;
  }

  private Leg streetLeg(TraverseMode mode, int startTime, int endTime, Place to, int legCost) {
    StreetLeg leg = new StreetLeg(
            mode,
            GregorianCalendar.from(newTime(startTime)),
            GregorianCalendar.from(newTime(endTime)),
            stop(lastPlace),
            stop(to),
            speed(mode) * (endTime - startTime),
            legCost,
            null,
            List.of()
    );

    legs.add(leg);
    cost += legCost;

    // Setup for adding another leg
    lastEndTime = endTime;
    lastPlace = to;

    return leg;
  }

  private double speed(TraverseMode mode) {
    switch (mode) {
      case WALK: return WALK_SPEED;
      case BICYCLE: return BICYCLE_SPEED;
      case BUS: return BUS_SPEED;
      case RAIL: return RAIL_SPEED;
      case CAR: return CAR_SPEED;
      default: throw new IllegalStateException("Unsupported mode: " + mode);
    }
  }

  private int cost(float reluctance, int durationSeconds) {
    return Math.round(reluctance * durationSeconds);
  }

  private int lastEndTime(int fallbackTime) {
    return lastEndTime == NOT_SET ? fallbackTime : lastEndTime;
  }

  /** Create a dummy trip */
  private  static Trip trip(int id, Route route) {
    Trip trip = new Trip(new FeedScopedId(FEED_ID, Integer.toString(id)));
    trip.setRoute(route);
    return trip;
  }

  /** Create a dummy route */
  private  static Route route(TransitMode mode) {
    Route route = new Route(new FeedScopedId(FEED_ID, mode.name()));
    route.setMode(mode);
    return route;
  }

  private static Place stop(Place source) {
    return Place.forStop(source.stop);
  }
}
