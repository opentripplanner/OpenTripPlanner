package org.opentripplanner.model.plan;

import static java.time.ZoneOffset.UTC;
import static org.opentripplanner.street.search.TraverseMode.BICYCLE;
import static org.opentripplanner.street.search.TraverseMode.CAR;
import static org.opentripplanner.street.search.TraverseMode.WALK;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.FEED_ID;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.route;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.ext.ridehailing.model.RideEstimate;
import org.opentripplanner.ext.ridehailing.model.RideHailingLeg;
import org.opentripplanner.ext.ridehailing.model.RideHailingProvider;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.utils.lang.IntUtils;
import org.opentripplanner.utils.time.TimeUtils;

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

  private static final int NOT_SET = -1_999_999;

  public static final LocalDate SERVICE_DAY = LocalDate.of(2020, Month.FEBRUARY, 2);
  public static final Route BUS_ROUTE = route("1").withMode(TransitMode.BUS).build();
  public static final Route RAIL_ROUTE = route("2").withMode(TransitMode.RAIL).build();

  /**
   * For Transit Legs the stopIndex in from/to palce should be increasing. We do not use/lookup the
   * pattern or trip in the unit tests using this, so the index is fiction. Some of the Itinerary
   * filters rely on it to make sure to trips overlap.
   */
  private static final int TRIP_FROM_STOP_INDEX = 5;
  private static final int TRIP_TO_STOP_INDEX = 7;
  private final List<Leg> legs = new ArrayList<>();
  private Place lastPlace;
  private int lastEndTime;
  private int c1 = 0;
  private boolean isSearchWindowAware = true;

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
   * Convert a seconds since midnight to a ZonedDateTime
   */
  public static ZonedDateTime newTime(int seconds) {
    return TimeUtils.zonedDateTime(SERVICE_DAY, seconds, UTC);
  }

  /**
   * Add a walking leg to the itinerary
   *
   * @param duration number of seconds to walk
   */
  public TestItineraryBuilder walk(int duration, Place to, List<WalkStep> steps) {
    if (lastEndTime == NOT_SET) {
      throw new IllegalStateException("Start time unknown!");
    }
    int legCost = cost(WALK_RELUCTANCE_FACTOR, duration);
    streetLeg(WALK, lastEndTime, lastEndTime + duration, to, legCost, steps);
    return this;
  }

  /**
   * Add a walking leg to the itinerary
   *
   * @param duration number of seconds to walk
   */
  public TestItineraryBuilder walk(int duration, Place to) {
    walk(duration, to, List.of());
    return this;
  }

  /**
   * Add a bicycle leg to the itinerary.
   */
  public TestItineraryBuilder bicycle(int startTime, int endTime, Place to) {
    int legCost = cost(BICYCLE_RELUCTANCE_FACTOR, endTime - startTime);
    streetLeg(BICYCLE, startTime, endTime, to, legCost, List.of());
    return this;
  }

  public TestItineraryBuilder drive(int startTime, int endTime, Place to) {
    int legCost = cost(CAR_RELUCTANCE_FACTOR, endTime - startTime);
    streetLeg(CAR, startTime, endTime, to, legCost, List.of());
    return this;
  }

  /**
   * Add a rented bicycle leg to the itinerary.
   */
  public TestItineraryBuilder rentedBicycle(int startTime, int endTime, Place to) {
    int legCost = cost(BICYCLE_RELUCTANCE_FACTOR, endTime - startTime);
    streetLeg(BICYCLE, startTime, endTime, to, legCost, List.of());
    var leg = ((StreetLeg) this.legs.get(0));
    var updatedLeg = leg.copyOf().withRentedVehicle(true).build();
    this.legs.add(0, updatedLeg);
    return this;
  }

  /**
   * Add a bus leg to the itinerary.
   */
  public TestItineraryBuilder bus(
    int tripId,
    int startTime,
    int endTime,
    int fromStopIndex,
    int toStopIndex,
    Place to,
    LocalDate serviceDate
  ) {
    return transit(
      BUS_ROUTE.copy().withShortName(Integer.toString(tripId)).build(),
      Integer.toString(tripId),
      startTime,
      endTime,
      fromStopIndex,
      toStopIndex,
      to,
      serviceDate,
      null,
      null
    );
  }

  public TestItineraryBuilder bus(int tripId, int startTime, int endTime, Place to) {
    return bus(tripId, startTime, endTime, TRIP_FROM_STOP_INDEX, TRIP_TO_STOP_INDEX, to, null);
  }

  public TestItineraryBuilder bus(
    int tripId,
    int startTime,
    int endTime,
    int fromStopIndex,
    int toStopIndex,
    Place to
  ) {
    return bus(tripId, startTime, endTime, fromStopIndex, toStopIndex, to, null);
  }

  public TestItineraryBuilder flex(int start, int end, Place to) {
    if (lastPlace == null) {
      throw new IllegalStateException("Trip from place is unknown!");
    }
    int legCost = 0;
    StopTime fromStopTime = new StopTime();
    fromStopTime.setStop(lastPlace.stop);
    fromStopTime.setFlexWindowStart(start);
    fromStopTime.setFlexWindowEnd(end);

    StopTime toStopTime = new StopTime();
    toStopTime.setStop(to.stop);
    toStopTime.setFlexWindowStart(start);
    toStopTime.setFlexWindowEnd(end);

    Trip trip = trip("1", route("flex").build());

    var flexTrip = UnscheduledTrip.of(id("flex-1"))
      .withStopTimes(List.of(fromStopTime, toStopTime))
      .withTrip(trip)
      .build();

    int fromStopPos = 0;
    int toStopPos = 1;
    LocalDate serviceDate = LocalDate.of(2024, Month.MAY, 22);

    var fromv = StreetModelForTest.intersectionVertex(
      "v1",
      lastPlace.coordinate.latitude(),
      lastPlace.coordinate.longitude()
    );
    var tov = StreetModelForTest.intersectionVertex(
      "v2",
      to.coordinate.latitude(),
      to.coordinate.longitude()
    );

    var flexPath = new DirectFlexPathCalculator()
      .calculateFlexPath(fromv, tov, fromStopPos, toStopPos);

    var edge = new FlexTripEdge(
      fromv,
      tov,
      lastPlace.stop,
      to.stop,
      flexTrip,
      fromStopPos,
      toStopPos,
      serviceDate,
      flexPath
    );

    FlexibleTransitLeg leg = FlexibleTransitLeg.of()
      .withFlexTripEdge(edge)
      .withStartTime(newTime(start))
      .withEndTime(newTime(end))
      .withGeneralizedCost(legCost)
      .build();

    legs.add(leg);
    c1 += legCost;

    // Setup for adding another leg
    lastEndTime = end;
    lastPlace = to;

    return this;
  }

  public TestItineraryBuilder bus(Route route, int tripId, int startTime, int endTime, Place to) {
    return transit(
      route,
      Integer.toString(tripId),
      startTime,
      endTime,
      TRIP_FROM_STOP_INDEX,
      TRIP_TO_STOP_INDEX,
      to,
      null,
      null,
      null
    );
  }

  public TestItineraryBuilder bus(
    int tripId,
    int startTime,
    int endTime,
    Place to,
    LocalDate serviceDay
  ) {
    return bus(
      tripId,
      startTime,
      endTime,
      TRIP_FROM_STOP_INDEX,
      TRIP_TO_STOP_INDEX,
      to,
      serviceDay
    );
  }

  /**
   * Add a rail/train leg to the itinerary
   */
  public TestItineraryBuilder rail(int tripId, int startTime, int endTime, Place to) {
    return transit(
      RAIL_ROUTE,
      Integer.toString(tripId),
      startTime,
      endTime,
      TRIP_FROM_STOP_INDEX,
      TRIP_TO_STOP_INDEX,
      to,
      null,
      null,
      null
    );
  }

  public TestItineraryBuilder faresV2Rail(
    int tripId,
    int startTime,
    int endTime,
    Place to,
    @Nullable FeedScopedId networkId
  ) {
    Route route = RAIL_ROUTE;
    if (networkId != null) {
      var builder = RAIL_ROUTE.copy();
      var group = GroupOfRoutes.of(networkId).build();
      builder.getGroupsOfRoutes().add(group);
      route = builder.build();
    }

    return transit(
      route,
      Integer.toString(tripId),
      startTime,
      endTime,
      TRIP_FROM_STOP_INDEX,
      TRIP_TO_STOP_INDEX,
      to,
      null,
      null,
      null
    );
  }

  public TestItineraryBuilder frequencyBus(int tripId, int startTime, int endTime, Place to) {
    return transit(
      RAIL_ROUTE,
      Integer.toString(tripId),
      startTime,
      endTime,
      TRIP_FROM_STOP_INDEX,
      TRIP_TO_STOP_INDEX,
      to,
      null,
      600,
      null
    );
  }

  public TestItineraryBuilder staySeatedBus(
    Route route,
    int tripId,
    int startTime,
    int endTime,
    Place to
  ) {
    return transit(
      route,
      Integer.toString(tripId),
      startTime,
      endTime,
      TRIP_FROM_STOP_INDEX,
      TRIP_TO_STOP_INDEX,
      to,
      null,
      null,
      new ConstrainedTransfer(null, null, null, TransferConstraint.of().staySeated().build())
    );
  }

  public TestItineraryBuilder carHail(int duration, Place to) {
    var streetLeg = streetLeg(CAR, lastEndTime, lastEndTime + duration, to, 1000, List.of());

    var rhl = new RideHailingLeg(
      streetLeg,
      RideHailingProvider.UBER,
      new RideEstimate(
        RideHailingProvider.UBER,
        Duration.ofSeconds(duration),
        Money.euros(10),
        Money.euros(20),
        "VW",
        "UberX"
      )
    );
    // the removal is necessary because the call to streetLeg() also adds a leg to the list
    // since we want to override this and there is no replace() method we remove the last leg
    // and re-add another one
    legs.remove(legs.size() - 1);
    legs.add(rhl);

    return this;
  }

  public TestItineraryBuilder withIsSearchWindowAware(boolean searchWindowAware) {
    this.isSearchWindowAware = searchWindowAware;
    return this;
  }

  public Itinerary egress(int walkDuration) {
    walk(walkDuration, null);
    return build();
  }

  public ItineraryBuilder itineraryBuilder() {
    ItineraryBuilder builder = isSearchWindowAware
      ? Itinerary.ofScheduledTransit(legs)
      : Itinerary.ofDirect(legs);

    builder.withGeneralizedCost(Cost.costOfSeconds(c1));

    return builder;
  }

  public Itinerary build() {
    return itineraryBuilder().build();
  }

  /**
   * Override any value set for c1. The given value will be assigned to the itinerary
   * independent of any values set on the legs.
   */
  public Itinerary build(int c1) {
    return itineraryBuilder().withGeneralizedCost(Cost.costOfSeconds(c1)).build();
  }

  /* private methods */

  /** Create a dummy trip */
  private static Trip trip(String id, Route route) {
    return TimetableRepositoryForTest.trip(id)
      .withRoute(route)
      .withHeadsign(I18NString.of("Trip headsign %s".formatted(id)))
      .build();
  }

  private static Place stop(Place source) {
    return Place.forStop(source.stop);
  }

  public TestItineraryBuilder transit(
    Route route,
    String tripId,
    int start,
    int end,
    int fromStopIndex,
    int toStopIndex,
    Place to,
    LocalDate serviceDate,
    Integer headwaySecs,
    ConstrainedTransfer transferFromPreviousLeg
  ) {
    if (lastPlace == null) {
      throw new IllegalStateException("Trip from place is unknown!");
    }
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
    fromStopTime.setStopHeadsign(
      I18NString.of("Headsign at boarding (stop index %s)".formatted(fromStopIndex))
    );

    // Duplicate stop time for all stops prior to the last.
    for (int i = 0; i < toStopIndex; i++) {
      stopTimes.add(fromStopTime);
    }

    StopTime toStopTime = new StopTime();
    toStopTime.setStop(to.stop);
    toStopTime.setArrivalTime(end);
    toStopTime.setDepartureTime(end);
    toStopTime.setTrip(trip);

    stopTimes.add(toStopTime);

    StopPattern stopPattern = new StopPattern(stopTimes);
    final TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());
    TripPattern tripPattern = TripPattern.of(route.getId())
      .withRoute(route)
      .withStopPattern(stopPattern)
      .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tripTimes))
      .build();

    ScheduledTransitLeg leg;

    var distance = speed(trip.getMode()) * (end - start);

    if (headwaySecs != null) {
      leg = new FrequencyTransitLegBuilder()
        .withTripTimes(tripTimes)
        .withTripPattern(tripPattern)
        .withBoardStopIndexInPattern(fromStopIndex)
        .withAlightStopIndexInPattern(toStopIndex)
        .withStartTime(newTime(start))
        .withEndTime(newTime(end))
        .withServiceDate(serviceDate != null ? serviceDate : SERVICE_DAY)
        .withZoneId(UTC)
        .withTransferFromPreviousLeg(transferFromPreviousLeg)
        .withGeneralizedCost(legCost)
        .withDistanceMeters(distance)
        .withFrequencyHeadwayInSeconds(headwaySecs)
        .build();
    } else {
      leg = new ScheduledTransitLegBuilder()
        .withTripTimes(tripTimes)
        .withTripPattern(tripPattern)
        .withBoardStopIndexInPattern(fromStopIndex)
        .withAlightStopIndexInPattern(toStopIndex)
        .withStartTime(newTime(start))
        .withEndTime(newTime(end))
        .withServiceDate(serviceDate != null ? serviceDate : SERVICE_DAY)
        .withZoneId(UTC)
        .withTransferFromPreviousLeg(transferFromPreviousLeg)
        .withGeneralizedCost(legCost)
        .withDistanceMeters(distance)
        .build();
    }

    legs.add(leg);
    c1 += legCost;

    // Setup for adding another leg
    lastEndTime = end;
    lastPlace = to;

    return this;
  }

  private StreetLeg streetLeg(
    TraverseMode mode,
    int startTime,
    int endTime,
    Place to,
    int legCost,
    List<WalkStep> walkSteps
  ) {
    StreetLeg leg = StreetLeg.of()
      .withMode(mode)
      .withStartTime(newTime(startTime))
      .withEndTime(newTime(endTime))
      .withFrom(stop(lastPlace))
      .withTo(stop(to))
      .withDistanceMeters(speed(mode) * (endTime - startTime))
      .withGeneralizedCost(legCost)
      .withWalkSteps(walkSteps)
      .build();

    legs.add(leg);
    c1 += legCost;

    // Setup for adding another leg
    lastEndTime = endTime;
    lastPlace = to;

    return leg;
  }

  private double speed(TransitMode mode) {
    return switch (mode) {
      case BUS -> BUS_SPEED;
      case RAIL -> RAIL_SPEED;
      default -> throw new IllegalStateException("Unsupported mode: " + mode);
    };
  }

  private double speed(TraverseMode mode) {
    return switch (mode) {
      case WALK -> WALK_SPEED;
      case BICYCLE -> BICYCLE_SPEED;
      case CAR -> CAR_SPEED;
      default -> throw new IllegalStateException("Unsupported mode: " + mode);
    };
  }

  private int cost(float reluctance, int durationSeconds) {
    return IntUtils.round(reluctance * durationSeconds);
  }

  private int lastEndTime(int fallbackTime) {
    return lastEndTime == NOT_SET ? fallbackTime : lastEndTime;
  }
}
