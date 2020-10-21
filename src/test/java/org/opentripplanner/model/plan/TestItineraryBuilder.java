package org.opentripplanner.model.plan;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import static java.util.Calendar.FEBRUARY;
import static org.opentripplanner.routing.core.TraverseMode.BICYCLE;
import static org.opentripplanner.routing.core.TraverseMode.WALK;

/**
 * This is a helper class to allow unit-testing on Itineraries. The builder does not necessarily
 * create complete/correct itineraries with legs - it is the unit-test witch is responsible to
 * create an itinerary that have correct data witch is consistent with what is produced by OTP. To
 * keep maintenance easy, create the minimum amount of data to focus your test - this also help
 * demonstrate which data is needed by the "code-under-test".
 */
public class TestItineraryBuilder {
  public static final ServiceDate SERVICE_DATE = new ServiceDate(2020, 9, 21);
  public static final String FEED = "F";
  public static final Route BUS_ROUTE = route(TransitMode.BUS);
  public static final Route RAIL_ROUTE = route(TransitMode.RAIL);

  public static final Place A = place("A", 5.0, 8.0 );
  public static final Place B = place("B", 6.0, 8.5);
  public static final Place C = place("C", 7.0, 9.0);
  public static final Place D = place("D", 8.0, 9.5);
  public static final Place E = place("E", 9.0, 10.0);
  public static final Place F = place("F", 9.0, 10.5);
  public static final Place G = place("G", 9.5, 11.0);

  /**
   * For Transit Legs the stopIndex in from/to palce should be increasing. We do not use/lookup
   * the pattern or trip in the unit tests using this, so the index is fiction. Some of the
   * Itinerary filters rely on it to make sure to trips overlap.
   */
  private static final int TRIP_FROM_STOP_INDEX = 5;
  private static final int TRIP_TO_STOP_INDEX = 7;

  private static final int NOT_SET = -999_999;
  public static final int BOARD_COST = 120;
  public static final float WALK_RELUCTANCE_FACTOR = 2.0f;
  public static final float BICYCLE_RELUCTANCE_FACTOR = 1.0f;
  public static final float WAIT_RELUCTANCE_FACTOR = 0.8f;
  public static final float WALK_SPEED = 1.4f;
  public static final float BICYCLE_SPEED = 5.0f;
  public static final float BUS_SPEED = 12.5f;
  public static final float RAIL_SPEED = 25.0f;

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
   * @param startTime  The number on minutes past noon. E.g. 123 is 14:03
   */
  public static TestItineraryBuilder newItinerary(Place origin, int startTime) {
    return new TestItineraryBuilder(origin, startTime);
  }

  /**
   * Add a walking leg to the itinerary
   * @param duration number of minutes to walk
   */
  public TestItineraryBuilder walk(int duration, Place to) {
    if(lastEndTime == NOT_SET) { throw new IllegalStateException("Start time unknown!"); }
    cost += cost(WALK_RELUCTANCE_FACTOR, duration);
    streetLeg(WALK, lastEndTime, lastEndTime + duration, to);
    return this;
  }

  /**
   * Add a bus leg to the itinerary.
   * @param start/end  The number on minutes past noon. E.g. 123 is 14:03
   */
  public TestItineraryBuilder bicycle(int start, int end, Place to) {
    cost += cost(BICYCLE_RELUCTANCE_FACTOR, end - start);
    streetLeg(BICYCLE, start, end, to);
    return this;
  }

  /**
   * Add a bus leg to the itinerary.
   * @param start/end  The number on minutes past noon. E.g. 123 is 14:03
   */
  public TestItineraryBuilder bus(int tripId, int start, int end, Place to) {
    return transit(BUS_ROUTE, tripId, start, end, to);
  }

  /**
   * Add a rail/train leg to the itinerary
   * @param start/end  The number on minutes past noon. E.g. 123 is 14:03
   */
  public TestItineraryBuilder rail(int tripId, int start, int end, Place to) {
    return transit(RAIL_ROUTE, tripId, start, end, to);
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

  public static GregorianCalendar newTime(int minutes) {
    int hours = 12 + minutes / 60;
    minutes = minutes % 60;
    return new GregorianCalendar(2020, FEBRUARY, 2, hours, minutes);
  }


  /* private methods */

  private TestItineraryBuilder transit(Route route, int tripId, int start, int end, Place to) {
    if(lastPlace == null) { throw new IllegalStateException("Trip from place is unknown!"); }
    int waitTime = start - lastEndTime(start);
    cost += cost(WAIT_RELUCTANCE_FACTOR, waitTime);
    cost += cost(1.0f, end - start) + BOARD_COST;
    Leg leg = leg(new Leg(trip(tripId, route)), start, end, to);
    leg.serviceDate = SERVICE_DATE;
    return this;
  }

  private Leg streetLeg(TraverseMode mode, int startTime, int endTime, Place to) {
    return leg(new Leg(mode), startTime, endTime, to);
  }

  private Leg leg(Leg leg, int startTime, int endTime, Place to) {
    leg.from = place(lastPlace, TRIP_FROM_STOP_INDEX);
    leg.startTime = newTime(startTime);
    leg.to = place(to, TRIP_TO_STOP_INDEX);
    leg.endTime = newTime(endTime);
    leg.distanceMeters = speed(leg.mode) * 60.0 * (endTime - startTime);
    legs.add(leg);

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
      default: throw new IllegalStateException("Unsupported mode: " + mode);
    }
  }

  private int cost(float reluctance, int durationMinutes) {
    return Math.round(reluctance * (60 * durationMinutes));
  }

  private int lastEndTime(int fallbackTime) {
    return lastEndTime == NOT_SET ? fallbackTime : lastEndTime;
  }

  private static Place place(String name, double lat, double lon) {
    Place p = new Place(lat, lon, name);
    p.stopId = new FeedScopedId(FEED, name);
    return p;
  }

  private static Place place(Place source, int stopIndex) {
    Place p = new Place(source.coordinate.latitude(), source.coordinate.longitude(), source.name);
    p.stopId = source.stopId;
    p.stopIndex = stopIndex;
    return p;
  }

  /** Create a dummy trip */
  private  static Trip trip(int id, Route route) {
    Trip trip = new Trip(new FeedScopedId(FEED, Integer.toString(id)));
    trip.setRoute(route);
    return trip;
  }

  /** Create a dummy route */
  private  static Route route(TransitMode mode) {
    Route route = new Route(new FeedScopedId(FEED, mode.name()));
    route.setMode(mode);
    route.setLongName(mode.name());
    return route;
  }
}
