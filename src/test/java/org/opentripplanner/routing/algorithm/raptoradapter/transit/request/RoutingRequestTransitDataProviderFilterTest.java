package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.WheelchairBoarding.NOT_POSSIBLE;
import static org.opentripplanner.model.WheelchairBoarding.POSSIBLE;

import java.time.LocalDate;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.model.BikeAccess;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripAlteration;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.WheelchairBoarding;
import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.model._data.TransitModelForTest;

public class RoutingRequestTransitDataProviderFilterTest {

  private static final FeedScopedId TEST_ROUTE_ID = TransitModelForTest.id("R1");

  private static final FeedScopedId TEST_TRIP_ID = TransitModelForTest.id("T1");

  private static final Stop STOP_FOR_TEST = Stop.stopForTest("TEST:STOP", 0, 0);

  private static final WheelchairAccessibilityRequest DEFAULT_ACCESSIBILITY =
    WheelchairAccessibilityRequest.DEFAULT;

  /**
   * Test filter for wheelchair access.
   *
   * @param wheelchair If true stops are wheelchair accessible else not
   */
  @ParameterizedTest
  @ValueSource(strings = { "true", "false" })
  public void testWheelchairAccess(boolean wheelchair) {
    var wheelchairBoarding = wheelchair ? POSSIBLE : NOT_POSSIBLE;

    var firstStop = Stop.stopForTest("TEST:START", wheelchairBoarding, 0.0, 0.0);
    var lastStop = Stop.stopForTest("TEST:END", wheelchairBoarding, 0.0, 0.0);

    var stopTimeStart = new StopTime();
    var stopTimeEnd = new StopTime();

    stopTimeStart.setStop(firstStop);
    stopTimeEnd.setStop(lastStop);

    var stopPattern = new StopPattern(List.of(stopTimeStart, stopTimeEnd));
    var pattern = new TripPattern(
      TransitModelForTest.id("P1"),
      new Route(TEST_ROUTE_ID),
      stopPattern
    );

    var tripPattern = new TripPatternWithRaptorStopIndexes(pattern, new int[2]);

    var filter = new RoutingRequestTransitDataProviderFilter(
      false,
      WheelchairAccessibilityRequest.makeDefault(true),
      false,
      Set.of(),
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

    var filter = new RoutingRequestTransitDataProviderFilter(
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      Set.of(AllowedTransitMode.fromMainModeEnum(TransitMode.BUS)),
      Set.of(),
      Set.of()
    );

    boolean valid = filter.tripPatternPredicate(tripPatternForDate);

    assertTrue(valid);
  }

  @Test
  public void bannedRouteFilteringTest() {
    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();

    var filter = new RoutingRequestTransitDataProviderFilter(
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      Set.of(AllowedTransitMode.fromMainModeEnum(TransitMode.BUS)),
      Set.of(TEST_ROUTE_ID),
      Set.of()
    );

    boolean valid = filter.tripPatternPredicate(tripPatternForDate);

    assertFalse(valid);
  }

  @Test
  public void bannedTripFilteringTest() {
    TripTimes tripTimes = createTestTripTimes(
      TEST_TRIP_ID,
      TEST_ROUTE_ID,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      NOT_POSSIBLE,
      null
    );

    var filter = new RoutingRequestTransitDataProviderFilter(
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      Set.of(AllowedTransitMode.fromMainModeEnum(TransitMode.BUS)),
      Set.of(),
      Set.of(TEST_TRIP_ID)
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertFalse(valid);
  }

  @Test
  public void transitModeFilteringTest() {
    TripTimes tripTimes = createTestTripTimes(
      TEST_TRIP_ID,
      TEST_ROUTE_ID,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      TransmodelTransportSubmode.LOCAL_BUS.getValue(),
      NOT_POSSIBLE,
      null
    );

    final var BUS = TransitMode.BUS;
    final var RAIL = TransitMode.RAIL;
    final var LOCAL_BUS = TransmodelTransportSubmode.LOCAL_BUS.getValue();
    final var REGIONAL_BUS = TransmodelTransportSubmode.REGIONAL_BUS.getValue();

    assertFalse(validateModesOnTripTimes(Set.of(), tripTimes));
    assertFalse(
      validateModesOnTripTimes(Set.of(new AllowedTransitMode(BUS, REGIONAL_BUS)), tripTimes)
    );
    assertFalse(
      validateModesOnTripTimes(Set.of(new AllowedTransitMode(RAIL, LOCAL_BUS)), tripTimes)
    );

    assertTrue(validateModesOnTripTimes(Set.of(new AllowedTransitMode(BUS, null)), tripTimes));
    assertTrue(validateModesOnTripTimes(Set.of(new AllowedTransitMode(BUS, LOCAL_BUS)), tripTimes));
  }

  @Test
  public void notFilteringExpectedTripTimesTest() {
    TripTimes tripTimes = createTestTripTimes(
      TEST_TRIP_ID,
      TEST_ROUTE_ID,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      NOT_POSSIBLE,
      null
    );

    var filter = new RoutingRequestTransitDataProviderFilter(
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      Set.of(AllowedTransitMode.fromMainModeEnum(TransitMode.BUS)),
      Set.of(),
      Set.of()
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertTrue(valid);
  }

  @Test
  public void bikesAllowedFilteringTest() {
    TripTimes tripTimes = createTestTripTimes(
      TEST_TRIP_ID,
      TEST_ROUTE_ID,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      NOT_POSSIBLE,
      null
    );

    var filter = new RoutingRequestTransitDataProviderFilter(
      true,
      DEFAULT_ACCESSIBILITY,
      false,
      AllowedTransitMode.getAllTransitModes(),
      Set.of(),
      Set.of()
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertFalse(valid);
  }

  @Test
  public void removeInaccessibleTrip() {
    TripTimes tripTimes = createTestTripTimes(
      TEST_TRIP_ID,
      TEST_ROUTE_ID,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      NOT_POSSIBLE,
      null
    );
    tripTimes.getTrip().setWheelchairBoarding(WheelchairBoarding.NOT_POSSIBLE);

    var filter = new RoutingRequestTransitDataProviderFilter(
      false,
      WheelchairAccessibilityRequest.makeDefault(true),
      false,
      AllowedTransitMode.getAllTransitModes(),
      Set.of(),
      Set.of()
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertFalse(valid);
  }

  @Test
  public void keepAccessibleTrip() {
    TripTimes wheelchairAccessibleTrip = createTestTripTimes(
      TEST_TRIP_ID,
      TEST_ROUTE_ID,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      POSSIBLE,
      TripAlteration.PLANNED
    );

    var filter = new RoutingRequestTransitDataProviderFilter(
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      Set.of(AllowedTransitMode.fromMainModeEnum(TransitMode.BUS)),
      Set.of(),
      Set.of()
    );

    boolean valid = filter.tripTimesPredicate(wheelchairAccessibleTrip);

    assertTrue(valid);
  }

  @Test
  public void includePlannedCancellationsTest() {
    TripTimes tripTimesWithCancellation = createTestTripTimes(
      TEST_TRIP_ID,
      TEST_ROUTE_ID,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      NOT_POSSIBLE,
      TripAlteration.CANCELLATION
    );
    TripTimes tripTimesWithReplaced = createTestTripTimes(
      TEST_TRIP_ID,
      TEST_ROUTE_ID,
      BikeAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      NOT_POSSIBLE,
      TripAlteration.REPLACED
    );

    // Given
    var filter1 = new RoutingRequestTransitDataProviderFilter(
      false,
      DEFAULT_ACCESSIBILITY,
      true,
      Set.of(AllowedTransitMode.fromMainModeEnum(TransitMode.BUS)),
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
    var filter2 = new RoutingRequestTransitDataProviderFilter(
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      Set.of(),
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
    Trip trip = new Trip(TransitModelForTest.id("T1"));
    Route route = new Route(TransitModelForTest.id("R1"));
    trip.setRoute(route);

    assertEquals(
      BikeAccess.UNKNOWN,
      RoutingRequestTransitDataProviderFilter.bikeAccessForTrip(trip)
    );
    trip.setBikesAllowed(BikeAccess.ALLOWED);
    assertEquals(
      BikeAccess.ALLOWED,
      RoutingRequestTransitDataProviderFilter.bikeAccessForTrip(trip)
    );
    trip.setBikesAllowed(BikeAccess.NOT_ALLOWED);
    assertEquals(
      BikeAccess.NOT_ALLOWED,
      RoutingRequestTransitDataProviderFilter.bikeAccessForTrip(trip)
    );
    route.setBikesAllowed(BikeAccess.ALLOWED);
    assertEquals(
      BikeAccess.NOT_ALLOWED,
      RoutingRequestTransitDataProviderFilter.bikeAccessForTrip(trip)
    );
    trip.setBikesAllowed(BikeAccess.UNKNOWN);
    assertEquals(
      BikeAccess.ALLOWED,
      RoutingRequestTransitDataProviderFilter.bikeAccessForTrip(trip)
    );
    route.setBikesAllowed(BikeAccess.NOT_ALLOWED);
    assertEquals(
      BikeAccess.NOT_ALLOWED,
      RoutingRequestTransitDataProviderFilter.bikeAccessForTrip(trip)
    );
  }

  @Test
  public void multipleFilteringTest() {
    TripTimes matchingTripTimes = createTestTripTimes(
      TEST_TRIP_ID,
      TEST_ROUTE_ID,
      BikeAccess.ALLOWED,
      TransitMode.BUS,
      null,
      POSSIBLE,
      TripAlteration.PLANNED
    );
    TripTimes failingTripTimes1 = createTestTripTimes(
      TEST_TRIP_ID,
      TEST_ROUTE_ID,
      BikeAccess.ALLOWED,
      TransitMode.RAIL,
      null,
      POSSIBLE,
      TripAlteration.PLANNED
    );
    TripTimes failingTripTimes2 = createTestTripTimes(
      TEST_TRIP_ID,
      TEST_ROUTE_ID,
      BikeAccess.NOT_ALLOWED,
      TransitMode.RAIL,
      null,
      POSSIBLE,
      TripAlteration.CANCELLATION
    );
    TripTimes failingTripTimes3 = createTestTripTimes(
      TEST_TRIP_ID,
      TEST_ROUTE_ID,
      BikeAccess.NOT_ALLOWED,
      TransitMode.RAIL,
      null,
      NOT_POSSIBLE,
      TripAlteration.CANCELLATION
    );
    TripTimes failingTripTimes4 = createTestTripTimes(
      TEST_TRIP_ID,
      TEST_ROUTE_ID,
      BikeAccess.ALLOWED,
      TransitMode.BUS,
      null,
      NOT_POSSIBLE,
      TripAlteration.PLANNED
    );
    TripTimes failingTripTimes5 = createTestTripTimes(
      TEST_TRIP_ID,
      TEST_ROUTE_ID,
      BikeAccess.ALLOWED,
      TransitMode.BUS,
      null,
      POSSIBLE,
      TripAlteration.CANCELLATION
    );

    var filter = new RoutingRequestTransitDataProviderFilter(
      true,
      WheelchairAccessibilityRequest.makeDefault(true),
      false,
      Set.of(AllowedTransitMode.fromMainModeEnum(TransitMode.BUS)),
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
    Set<AllowedTransitMode> allowedModes,
    TripTimes tripTimes
  ) {
    var filter = new RoutingRequestTransitDataProviderFilter(
      false,
      WheelchairAccessibilityRequest.makeDefault(false),
      false,
      allowedModes,
      Set.of(),
      Set.of()
    );

    return filter.tripTimesPredicate(tripTimes);
  }

  private TripPatternForDate createTestTripPatternForDate() {
    Route route = new Route(TEST_ROUTE_ID);
    route.setMode(TransitMode.BUS);

    var stopTime = new StopTime();
    stopTime.setStop(STOP_FOR_TEST);
    StopPattern stopPattern = new StopPattern(List.of(stopTime));
    TripPattern pattern = new TripPattern(TransitModelForTest.id("P1"), route, stopPattern);

    TripPatternWithRaptorStopIndexes tripPattern = new TripPatternWithRaptorStopIndexes(
      pattern,
      new int[0]
    );

    TripTimes tripTimes = Mockito.mock(TripTimes.class);

    return new TripPatternForDate(tripPattern, List.of(tripTimes), List.of(), LocalDate.now());
  }

  private TripTimes createTestTripTimes(
    FeedScopedId tripId,
    FeedScopedId routeId,
    BikeAccess bikeAccess,
    TransitMode mode,
    String submode,
    WheelchairBoarding wheelchairBoarding,
    TripAlteration tripAlteration
  ) {
    Trip trip = new Trip(tripId);
    trip.setRoute(new Route(routeId));
    trip.setMode(mode);
    trip.setNetexSubmode(submode);
    trip.setBikesAllowed(bikeAccess);
    trip.setWheelchairBoarding(wheelchairBoarding);
    trip.setAlteration(tripAlteration);

    StopTime stopTime = new StopTime();
    stopTime.setStop(STOP_FOR_TEST);
    stopTime.setArrivalTime(60);
    stopTime.setDepartureTime(60);
    stopTime.setStopSequence(0);

    return new TripTimes(trip, List.of(stopTime), new Deduplicator());
  }
}
