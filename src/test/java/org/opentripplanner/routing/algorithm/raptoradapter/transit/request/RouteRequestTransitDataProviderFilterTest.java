package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RouteBuilder;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.transit.model.timetable.TripBuilder;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class RouteRequestTransitDataProviderFilterTest {

  private static final Route ROUTE = TransitModelForTest.route("1").build();

  private static final FeedScopedId TRIP_ID = TransitModelForTest.id("T1");

  private static final RegularStop STOP_FOR_TEST = TransitModelForTest.stopForTest(
    "TEST:STOP",
    0,
    0
  );

  private static final WheelchairPreferences DEFAULT_ACCESSIBILITY = WheelchairPreferences.DEFAULT;

  /**
   * Test filter for wheelchair access.
   *
   * @param wheelchair If true stops are wheelchair accessible else not
   */
  @ParameterizedTest
  @ValueSource(strings = { "true", "false" })
  public void testWheelchairAccess(boolean wheelchair) {
    var wheelchairBoarding = wheelchair ? Accessibility.POSSIBLE : Accessibility.NOT_POSSIBLE;

    var firstStop = TransitModelForTest.stopForTest("TEST:START", wheelchairBoarding, 0.0, 0.0);
    var lastStop = TransitModelForTest.stopForTest("TEST:END", wheelchairBoarding, 0.0, 0.0);

    var stopTimeStart = new StopTime();
    var stopTimeEnd = new StopTime();

    stopTimeStart.setStop(firstStop);
    stopTimeEnd.setStop(lastStop);

    var stopPattern = new StopPattern(List.of(stopTimeStart, stopTimeEnd));
    var tripPattern = TripPattern
      .of(TransitModelForTest.id("P1"))
      .withRoute(TransitModelForTest.route("1").build())
      .withStopPattern(stopPattern)
      .build()
      .getRoutingTripPattern();

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      true,
      DEFAULT_ACCESSIBILITY,
      false,
      MainAndSubMode.all(),
      Set.of(),
      Set.of()
    );

    var boardingPossible = new BitSet();

    boardingPossible.set(0, 2);

    var wheelchairPossible = filter.filterAvailableStops(tripPattern, boardingPossible);

    assertEquals(wheelchair, wheelchairPossible.get(0), "Wrong boarding value on first stop");
    assertEquals(wheelchair, wheelchairPossible.get(1), "Wrong boarding value on second stop");
  }

  @Test
  public void notFilteringExpectedTripPatternForDateTest() {
    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      List.of(new MainAndSubMode(TransitMode.BUS)),
      Set.of(),
      Set.of()
    );

    boolean valid = filter.tripPatternPredicate(tripPatternForDate);

    assertTrue(valid);
  }

  @Test
  public void bannedRouteFilteringTest() {
    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      List.of(new MainAndSubMode(TransitMode.BUS)),
      Set.of(ROUTE.getId()),
      Set.of()
    );

    boolean valid = filter.tripPatternPredicate(tripPatternForDate);

    assertFalse(valid);
  }

  @Test
  public void bannedTripFilteringTest() {
    TripTimes tripTimes = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      null
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      List.of(new MainAndSubMode(TransitMode.BUS)),
      Set.of(),
      Set.of(TRIP_ID)
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertFalse(valid);
  }

  @Test
  public void transitModeFilteringTest() {
    TripTimes tripTimes = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      TransmodelTransportSubmode.LOCAL_BUS.getValue(),
      Accessibility.NOT_POSSIBLE,
      null
    );

    final var BUS = TransitMode.BUS;
    final var RAIL = TransitMode.RAIL;
    final var LOCAL_BUS = SubMode.of(TransmodelTransportSubmode.LOCAL_BUS.getValue());
    final var REGIONAL_BUS = SubMode.of(TransmodelTransportSubmode.REGIONAL_BUS.getValue());

    assertFalse(
      validateModesOnTripTimes(List.of(new MainAndSubMode(BUS, REGIONAL_BUS)), tripTimes)
    );
    assertFalse(validateModesOnTripTimes(List.of(new MainAndSubMode(RAIL, LOCAL_BUS)), tripTimes));

    assertTrue(validateModesOnTripTimes(List.of(new MainAndSubMode(BUS)), tripTimes));
    assertTrue(validateModesOnTripTimes(List.of(new MainAndSubMode(BUS, LOCAL_BUS)), tripTimes));
  }

  @Test
  public void notFilteringExpectedTripTimesTest() {
    TripTimes tripTimes = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      null
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      List.of(new MainAndSubMode(TransitMode.BUS)),
      Set.of(),
      Set.of()
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertTrue(valid);
  }

  @Test
  public void bikesAllowedFilteringTest() {
    TripTimes tripTimes = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      null
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      true,
      true,
      WheelchairPreferences.DEFAULT,
      false,
      MainAndSubMode.all(),
      Set.of(),
      Set.of()
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertFalse(valid);
  }

  @Test
  public void removeInaccessibleTrip() {
    TripTimes tripTimes = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      null
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      true,
      WheelchairPreferences.DEFAULT,
      false,
      MainAndSubMode.all(),
      Set.of(),
      Set.of()
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertFalse(valid);
  }

  @Test
  public void keepAccessibleTrip() {
    TripTimes wheelchairAccessibleTrip = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.PLANNED
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      true,
      WheelchairPreferences.DEFAULT,
      false,
      List.of(new MainAndSubMode(TransitMode.BUS)),
      Set.of(),
      Set.of()
    );

    boolean valid = filter.tripTimesPredicate(wheelchairAccessibleTrip);

    assertTrue(valid);
  }

  @Test
  public void keepRealTimeAccessibleTrip() {
    TripTimes realTimeWheelchairAccessibleTrip = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.PLANNED
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      true,
      WheelchairPreferences.DEFAULT,
      false,
      List.of(new MainAndSubMode(TransitMode.BUS)),
      Set.of(),
      Set.of()
    );

    assertFalse(filter.tripTimesPredicate(realTimeWheelchairAccessibleTrip));

    realTimeWheelchairAccessibleTrip.updateWheelchairAccessibility(Accessibility.POSSIBLE);

    assertTrue(filter.tripTimesPredicate(realTimeWheelchairAccessibleTrip));
  }

  @Test
  public void includePlannedCancellationsTest() {
    TripTimes tripTimesWithCancellation = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.CANCELLATION
    );
    TripTimes tripTimesWithReplaced = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.REPLACED
    );

    // Given
    var filter1 = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      WheelchairPreferences.DEFAULT,
      true,
      List.of(new MainAndSubMode(TransitMode.BUS)),
      Set.of(),
      Set.of()
    );

    // When
    boolean valid1 = filter1.tripTimesPredicate(tripTimesWithCancellation);
    // Then
    assertTrue(valid1);

    // When
    boolean valid2 = filter1.tripTimesPredicate(tripTimesWithReplaced);
    // Then
    assertTrue(valid2);

    // Given
    var filter2 = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      MainAndSubMode.all(),
      Set.of(),
      Set.of()
    );

    // When
    boolean valid3 = filter2.tripTimesPredicate(tripTimesWithCancellation);
    // Then
    assertFalse(valid3);

    // When
    boolean valid4 = filter2.tripTimesPredicate(tripTimesWithReplaced);
    // Then
    assertFalse(valid4);
  }

  @Test
  public void testBikesAllowed() {
    RouteBuilder routeBuilder = TransitModelForTest.route("1");
    TripBuilder trip = Trip.of(TransitModelForTest.id("T1")).withRoute(routeBuilder.build());

    assertEquals(
      BikeAccess.UNKNOWN,
      RouteRequestTransitDataProviderFilter.bikeAccessForTrip(trip.build())
    );
    trip.withBikesAllowed(BikeAccess.ALLOWED);
    assertEquals(
      BikeAccess.ALLOWED,
      RouteRequestTransitDataProviderFilter.bikeAccessForTrip(trip.build())
    );
    trip.withBikesAllowed(BikeAccess.NOT_ALLOWED);
    assertEquals(
      BikeAccess.NOT_ALLOWED,
      RouteRequestTransitDataProviderFilter.bikeAccessForTrip(trip.build())
    );
    trip.withRoute(routeBuilder.withBikesAllowed(BikeAccess.ALLOWED).build());
    assertEquals(
      BikeAccess.NOT_ALLOWED,
      RouteRequestTransitDataProviderFilter.bikeAccessForTrip(trip.build())
    );
    trip.withBikesAllowed(BikeAccess.UNKNOWN);
    assertEquals(
      BikeAccess.ALLOWED,
      RouteRequestTransitDataProviderFilter.bikeAccessForTrip(trip.build())
    );
    trip.withRoute(routeBuilder.withBikesAllowed(BikeAccess.NOT_ALLOWED).build());
    assertEquals(
      BikeAccess.NOT_ALLOWED,
      RouteRequestTransitDataProviderFilter.bikeAccessForTrip(trip.build())
    );
  }

  @Test
  public void multipleFilteringTest() {
    TripTimes matchingTripTimes = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.PLANNED
    );
    TripTimes failingTripTimes1 = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.ALLOWED,
      TransitMode.RAIL,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.PLANNED
    );
    TripTimes failingTripTimes2 = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      TransitMode.RAIL,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.CANCELLATION
    );
    TripTimes failingTripTimes3 = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      TransitMode.RAIL,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.CANCELLATION
    );
    TripTimes failingTripTimes4 = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.PLANNED
    );
    TripTimes failingTripTimes5 = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.CANCELLATION
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      true,
      true,
      DEFAULT_ACCESSIBILITY,
      false,
      List.of(new MainAndSubMode(TransitMode.BUS)),
      Set.of(),
      Set.of()
    );

    assertTrue(filter.tripTimesPredicate(matchingTripTimes));

    assertFalse(filter.tripTimesPredicate(failingTripTimes1));
    assertFalse(filter.tripTimesPredicate(failingTripTimes2));
    assertFalse(filter.tripTimesPredicate(failingTripTimes3));
    assertFalse(filter.tripTimesPredicate(failingTripTimes4));
    assertFalse(filter.tripTimesPredicate(failingTripTimes5));
  }

  private boolean validateModesOnTripTimes(
    Collection<MainAndSubMode> allowedModes,
    TripTimes tripTimes
  ) {
    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      allowedModes,
      Set.of(),
      Set.of()
    );

    return filter.tripTimesPredicate(tripTimes);
  }

  private TripPatternForDate createTestTripPatternForDate() {
    Route route = TransitModelForTest.route("1").build();

    var stopTime = new StopTime();
    stopTime.setStop(STOP_FOR_TEST);
    StopPattern stopPattern = new StopPattern(List.of(stopTime));
    RoutingTripPattern tripPattern = TripPattern
      .of(TransitModelForTest.id("P1"))
      .withRoute(route)
      .withStopPattern(stopPattern)
      .build()
      .getRoutingTripPattern();

    TripTimes tripTimes = Mockito.mock(TripTimes.class);

    return new TripPatternForDate(tripPattern, List.of(tripTimes), List.of(), LocalDate.now());
  }

  private TripTimes createTestTripTimes(
    FeedScopedId tripId,
    Route route,
    BikeAccess bikeAccess,
    TransitMode mode,
    String submode,
    Accessibility wheelchairBoarding,
    TripAlteration tripAlteration
  ) {
    Trip trip = Trip
      .of(tripId)
      .withRoute(route)
      .withMode(mode)
      .withNetexSubmode(submode)
      .withBikesAllowed(bikeAccess)
      .withWheelchairBoarding(wheelchairBoarding)
      .withNetexAlteration(tripAlteration)
      .build();

    StopTime stopTime = new StopTime();
    stopTime.setStop(STOP_FOR_TEST);
    stopTime.setArrivalTime(60);
    stopTime.setDepartureTime(60);
    stopTime.setStopSequence(0);

    return new TripTimes(trip, List.of(stopTime), new Deduplicator());
  }
}
