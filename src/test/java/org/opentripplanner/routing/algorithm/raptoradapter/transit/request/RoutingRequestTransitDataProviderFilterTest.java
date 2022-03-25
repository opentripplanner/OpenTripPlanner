package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
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
import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;

public class RoutingRequestTransitDataProviderFilterTest {

  private static final FeedScopedId TEST_ROUTE_ID = new FeedScopedId("TEST", "ROUTE");

  private static final FeedScopedId TEST_TRIP_ID = new FeedScopedId("TEST", "TRIP");

  private static final Stop STOP_FOR_TEST = Stop.stopForTest("TEST:STOP", 0, 0);

  @Test
  public void notFilteringExpectedTripPatternForDateTest() {
    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();

    var filter = new RoutingRequestTransitDataProviderFilter(
        false,
        false,
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
        false,
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
            TEST_TRIP_ID, TEST_ROUTE_ID, BikeAccess.NOT_ALLOWED, TransitMode.BUS, null, 0, null
    );

    var filter = new RoutingRequestTransitDataProviderFilter(
            false,
            false,
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
            TEST_TRIP_ID, TEST_ROUTE_ID, BikeAccess.NOT_ALLOWED, TransitMode.BUS, TransmodelTransportSubmode.LOCAL_BUS.getValue(), 0, null
    );

    final var BUS = TransitMode.BUS;
    final var RAIL = TransitMode.RAIL;
    final var LOCAL_BUS = TransmodelTransportSubmode.LOCAL_BUS.getValue();
    final var REGIONAL_BUS = TransmodelTransportSubmode.REGIONAL_BUS.getValue();

    assertFalse(validateModesOnTripTimes(Set.of(), tripTimes));
    assertFalse(validateModesOnTripTimes(Set.of(new AllowedTransitMode(BUS, REGIONAL_BUS)), tripTimes));
    assertFalse(validateModesOnTripTimes(Set.of(new AllowedTransitMode(RAIL, LOCAL_BUS)), tripTimes));

    assertTrue(validateModesOnTripTimes(Set.of(new AllowedTransitMode(BUS, null)), tripTimes));
    assertTrue(validateModesOnTripTimes(Set.of(new AllowedTransitMode(BUS, LOCAL_BUS)), tripTimes));
  }

  private boolean validateModesOnTripTimes(Set<AllowedTransitMode> allowedModes, TripTimes tripTimes) {
    var filter = new RoutingRequestTransitDataProviderFilter(
            false,
            false,
            false,
            allowedModes,
            Set.of(),
            Set.of()
    );

    return filter.tripTimesPredicate(tripTimes);
  }

  @Test
  public void notFilteringExpectedTripTimesTest() {
    TripTimes tripTimes = createTestTripTimes(
            TEST_TRIP_ID, TEST_ROUTE_ID, BikeAccess.NOT_ALLOWED, TransitMode.BUS, null, 0, null
    );

    var filter = new RoutingRequestTransitDataProviderFilter(
        false,
        false,
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
            TEST_TRIP_ID, TEST_ROUTE_ID, BikeAccess.NOT_ALLOWED, TransitMode.BUS, null, 0, null
    );

    var filter = new RoutingRequestTransitDataProviderFilter(
        true,
        false,
        false,
        AllowedTransitMode.getAllTransitModes(),
        Set.of(),
        Set.of()
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertFalse(valid);
  }

  @Test
  public void wheelchairAccessibleFilteringTest() {
    TripTimes tripTimes = createTestTripTimes(
            TEST_TRIP_ID, TEST_ROUTE_ID, BikeAccess.NOT_ALLOWED, TransitMode.BUS, null, 0, null
    );

    var filter = new RoutingRequestTransitDataProviderFilter(
        false,
        true,
        false,
        AllowedTransitMode.getAllTransitModes(),
        Set.of(),
        Set.of()
    );

    boolean valid = filter.tripTimesPredicate(tripTimes);

    assertFalse(valid);
  }

  @Test
  public void includePlannedCancellationsTest() {
    TripTimes tripTimesWithCancellation = createTestTripTimes(
            TEST_TRIP_ID, TEST_ROUTE_ID, BikeAccess.NOT_ALLOWED, TransitMode.BUS, null, 0, TripAlteration.CANCELLATION
    );
    TripTimes tripTimesWithReplaced = createTestTripTimes(
            TEST_TRIP_ID, TEST_ROUTE_ID, BikeAccess.NOT_ALLOWED, TransitMode.BUS, null, 0, TripAlteration.REPLACED
    );

    // Given
    var filter1 = new RoutingRequestTransitDataProviderFilter(
        false,
        false,
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
        false,
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
    String FEED_ID = "F";
    Trip trip = new Trip(new FeedScopedId(FEED_ID, "T1"));
    Route route = new Route(new FeedScopedId(FEED_ID, "R1"));
    trip.setRoute(route);

    assertEquals(BikeAccess.UNKNOWN, RoutingRequestTransitDataProviderFilter.bikeAccessForTrip(trip));
    trip.setBikesAllowed(BikeAccess.ALLOWED);
    assertEquals(BikeAccess.ALLOWED, RoutingRequestTransitDataProviderFilter.bikeAccessForTrip(trip));
    trip.setBikesAllowed(BikeAccess.NOT_ALLOWED);
    assertEquals(BikeAccess.NOT_ALLOWED, RoutingRequestTransitDataProviderFilter.bikeAccessForTrip(trip));
    route.setBikesAllowed(BikeAccess.ALLOWED);
    assertEquals(BikeAccess.NOT_ALLOWED, RoutingRequestTransitDataProviderFilter.bikeAccessForTrip(trip));
    trip.setBikesAllowed(BikeAccess.UNKNOWN);
    assertEquals(BikeAccess.ALLOWED, RoutingRequestTransitDataProviderFilter.bikeAccessForTrip(trip));
    route.setBikesAllowed(BikeAccess.NOT_ALLOWED);
    assertEquals(BikeAccess.NOT_ALLOWED, RoutingRequestTransitDataProviderFilter.bikeAccessForTrip(trip));
  }

  @Test
  public void multipleFilteringTest() {
    TripTimes matchingTripTimes = createTestTripTimes(
            TEST_TRIP_ID, TEST_ROUTE_ID, BikeAccess.ALLOWED, TransitMode.BUS, null, 1, TripAlteration.PLANNED
    );
    TripTimes failingTripTimes1 = createTestTripTimes(
            TEST_TRIP_ID, TEST_ROUTE_ID, BikeAccess.ALLOWED, TransitMode.RAIL, null, 1, TripAlteration.PLANNED
    );
    TripTimes failingTripTimes2 = createTestTripTimes(
            TEST_TRIP_ID, TEST_ROUTE_ID, BikeAccess.NOT_ALLOWED, TransitMode.RAIL, null, 1, TripAlteration.CANCELLATION
    );
    TripTimes failingTripTimes3 = createTestTripTimes(
            TEST_TRIP_ID, TEST_ROUTE_ID, BikeAccess.NOT_ALLOWED, TransitMode.RAIL, null, 0, TripAlteration.CANCELLATION
    );
    TripTimes failingTripTimes4 = createTestTripTimes(
            TEST_TRIP_ID, TEST_ROUTE_ID, BikeAccess.ALLOWED, TransitMode.BUS, null, 0, TripAlteration.PLANNED
    );
    TripTimes failingTripTimes5 = createTestTripTimes(
            TEST_TRIP_ID, TEST_ROUTE_ID, BikeAccess.ALLOWED, TransitMode.BUS, null, 1, TripAlteration.CANCELLATION
    );

    var filter = new RoutingRequestTransitDataProviderFilter(
            true,
            true,
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

  private TripPatternForDate createTestTripPatternForDate() {
    Route route = new Route(TEST_ROUTE_ID);
    route.setMode(TransitMode.BUS);

    var stopTime = new StopTime();
    stopTime.setStop(STOP_FOR_TEST);
    StopPattern stopPattern = new StopPattern(List.of(stopTime));
    TripPattern pattern = new TripPattern(null, route, stopPattern);

    TripPatternWithRaptorStopIndexes tripPattern = new TripPatternWithRaptorStopIndexes(
            pattern, new int[0]
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
          int wheelchairAccessible,
          TripAlteration tripAlteration
  ) {
    Trip trip = new Trip(tripId);
    trip.setRoute(new Route(routeId));
    trip.setMode(mode);
    trip.setNetexSubmode(submode);
    trip.setBikesAllowed(bikeAccess);
    trip.setWheelchairAccessible(wheelchairAccessible);
    trip.setAlteration(tripAlteration);

    StopTime stopTime = new StopTime();
    stopTime.setStop(STOP_FOR_TEST);
    stopTime.setArrivalTime(60);
    stopTime.setDepartureTime(60);
    stopTime.setStopSequence(0);

    return new TripTimes(trip, List.of(stopTime), new Deduplicator());
  }
}
