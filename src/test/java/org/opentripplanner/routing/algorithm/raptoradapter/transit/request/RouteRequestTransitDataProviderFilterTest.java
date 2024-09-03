package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.CarAccess;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RouteBuilder;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.transit.model.timetable.TripBuilder;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class RouteRequestTransitDataProviderFilterTest {

  private static final TransitModelForTest TEST_MODEL = TransitModelForTest.of();

  private static final Route ROUTE = TransitModelForTest.route("1").build();

  private static final FeedScopedId TRIP_ID = TransitModelForTest.id("T1");

  private static final RegularStop STOP_FOR_TEST = TEST_MODEL.stop("TEST:STOP", 0, 0).build();

  private static final WheelchairPreferences DEFAULT_ACCESSIBILITY = WheelchairPreferences.DEFAULT;

  private static final AccessibilityPreferences RELAXED_ACCESSIBILITY_PREFERENCE = AccessibilityPreferences.ofCost(
    0,
    10
  );
  private static final WheelchairPreferences RELAXED_ACCESSIBILITY = WheelchairPreferences
    .of()
    .withTrip(RELAXED_ACCESSIBILITY_PREFERENCE)
    .withStop(RELAXED_ACCESSIBILITY_PREFERENCE)
    .withElevator(RELAXED_ACCESSIBILITY_PREFERENCE)
    .build();

  static Stream<Arguments> wheelchairCases() {
    return Stream.of(
      Arguments.of(Accessibility.POSSIBLE, DEFAULT_ACCESSIBILITY),
      Arguments.of(Accessibility.POSSIBLE, RELAXED_ACCESSIBILITY),
      Arguments.of(Accessibility.NOT_POSSIBLE, DEFAULT_ACCESSIBILITY),
      Arguments.of(Accessibility.NOT_POSSIBLE, RELAXED_ACCESSIBILITY),
      Arguments.of(Accessibility.NO_INFORMATION, DEFAULT_ACCESSIBILITY),
      Arguments.of(Accessibility.NO_INFORMATION, RELAXED_ACCESSIBILITY)
    );
  }

  /**
   * Test filter for wheelchair access.
   *
   * @param wheelchair Accessibility for stops
   */
  @ParameterizedTest
  @MethodSource("wheelchairCases")
  void testWheelchairAccess(Accessibility wheelchair, WheelchairPreferences accessibility) {
    var firstStop = stopForTest("TEST:START", wheelchair, 0.0, 0.0);
    var lastStop = stopForTest("TEST:END", wheelchair, 0.0, 0.0);

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
      false,
      true,
      accessibility,
      false,
      false,
      Set.of(),
      List.of(AllowAllTransitFilter.of())
    );

    var boardingPossible = new BitSet();

    boardingPossible.set(0, 2);

    var wheelchairPossible = filter.filterAvailableStops(
      tripPattern,
      boardingPossible,
      BoardAlight.BOARD
    );

    var relaxedAccessibility = !accessibility.stop().onlyConsiderAccessible();
    var wheelchairPossibleForStop = wheelchair == Accessibility.POSSIBLE;

    assertEquals(
      relaxedAccessibility || wheelchairPossibleForStop,
      wheelchairPossible.get(0),
      "Wrong boarding value on first stop"
    );
    assertEquals(
      relaxedAccessibility || wheelchairPossibleForStop,
      wheelchairPossible.get(1),
      "Wrong boarding value on second stop"
    );
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void testRealtimeCancelledStops(boolean includeRealtimeCancellations) {
    var stopTime1 = getStopTime("TEST:1", PickDrop.SCHEDULED);
    var stopTime2 = getStopTime("TEST:2", PickDrop.CANCELLED);
    var stopTime3 = getStopTime("TEST:3", PickDrop.NONE);
    var stopTime4 = getStopTime("TEST:4", PickDrop.SCHEDULED);
    var stopPattern = new StopPattern(List.of(stopTime1, stopTime2, stopTime3, stopTime4));
    var tripPattern = TripPattern
      .of(TransitModelForTest.id("P1"))
      .withRoute(TransitModelForTest.route("1").build())
      .withStopPattern(stopPattern)
      .build()
      .getRoutingTripPattern();

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      includeRealtimeCancellations,
      Set.of(),
      List.of(AllowAllTransitFilter.of())
    );

    var boardingPossible = tripPattern.getBoardingPossible();
    var boardingPossibleCopy = boardingPossible.clone();
    var filteredBoardingPossible = filter.filterAvailableStops(
      tripPattern,
      boardingPossible,
      BoardAlight.BOARD
    );

    assertTrue(filteredBoardingPossible.get(0));
    assertEquals(includeRealtimeCancellations, filteredBoardingPossible.get(1));
    assertFalse(filteredBoardingPossible.get(2));
    assertTrue(filteredBoardingPossible.get(3));

    assertEquals(boardingPossibleCopy, boardingPossible, "Method should not modify bitset");

    var alightingPossible = tripPattern.getAlightingPossible();
    var alightingPossibleCopy = boardingPossible.clone();
    var filteredAlightingPossible = filter.filterAvailableStops(
      tripPattern,
      tripPattern.getAlightingPossible(),
      BoardAlight.ALIGHT
    );

    assertTrue(filteredAlightingPossible.get(0));
    assertEquals(includeRealtimeCancellations, filteredAlightingPossible.get(1));
    assertFalse(filteredAlightingPossible.get(2));
    assertTrue(filteredAlightingPossible.get(3));

    assertEquals(alightingPossibleCopy, alightingPossible, "Method should not modify bitset");
  }

  @Test
  void notFilteringExpectedTripPatternForDateTest() {
    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      false,
      Set.of(),
      filterForMode(TransitMode.BUS)
    );

    boolean valid = filter.tripPatternPredicate(tripPatternForDate);

    assertTrue(valid);
  }

  @Test
  void bannedRouteFilteringTest() {
    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      false,
      Set.of(),
      List.of(
        TransitFilterRequest
          .of()
          .addNot(SelectRequest.of().withRoutes(List.of(ROUTE.getId())).build())
          .build()
      )
    );

    boolean valid = filter.tripPatternPredicate(tripPatternForDate);

    assertFalse(valid);
  }

  @Test
  void bannedTripFilteringTest() {
    TripTimes tripTimes = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      null
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      false,
      Set.of(TRIP_ID),
      filterForMode(TransitMode.BUS)
    );

    boolean valid = filter.tripTimesPredicate(tripTimes, true);

    assertFalse(valid);
  }

  /**
   * Filtering trips with 2 filters: a selection by submode filter and an exclusion by agency filter.
   * A trip matches if it matches either of them.
   */
  @Test
  void matchModeFilterAndBannedAgencyFilter() {
    TripTimes tripTimesMatchingSubModeMatchingAgency = createTestTripTimesWithSubmode(
      TransmodelTransportSubmode.UNKNOWN.getValue()
    );
    TripTimes tripTimesFailingSubModeMatchingAgency = createTestTripTimesWithSubmode(
      TransmodelTransportSubmode.LOCAL.getValue()
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      false,
      Set.of(),
      filterForModesAndFilterForBannedAgencies(
        List.of(
          new MainAndSubMode(
            TransitMode.BUS,
            SubMode.of(TransmodelTransportSubmode.UNKNOWN.getValue())
          )
        ),
        List.of(TransitModelForTest.OTHER_AGENCY.getId())
      )
    );

    assertTrue(filter.tripTimesPredicate(tripTimesMatchingSubModeMatchingAgency, true));
    assertTrue(filter.tripTimesPredicate(tripTimesFailingSubModeMatchingAgency, true));
  }

  /**
   * Filtering trips with one filter combining selection by submode amd exclusion by agency:
   * a trip matches if it matches both of them.
   */
  @Test
  void matchCombinedModesAndBannedAgencyFilter() {
    TripTimes tripTimesMatchingSubModeMatchingAgency = createTestTripTimesWithSubmode(
      TransmodelTransportSubmode.UNKNOWN.getValue()
    );
    TripTimes tripTimesFailingSubModeMatchingAgency = createTestTripTimesWithSubmode(
      TransmodelTransportSubmode.LOCAL.getValue()
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      false,
      Set.of(),
      combinedFilterForModesAndBannedAgencies(
        List.of(
          new MainAndSubMode(
            TransitMode.BUS,
            SubMode.of(TransmodelTransportSubmode.UNKNOWN.getValue())
          )
        ),
        List.of(TransitModelForTest.OTHER_AGENCY.getId())
      )
    );

    assertTrue(filter.tripTimesPredicate(tripTimesMatchingSubModeMatchingAgency, true));
    assertFalse(filter.tripTimesPredicate(tripTimesFailingSubModeMatchingAgency, true));
  }

  @Test
  void matchSelectedAgencyExcludedSubMode() {
    TripTimes tripTimesMatchingSubModeMatchingAgency = createTestTripTimesWithSubmode(
      TransmodelTransportSubmode.UNKNOWN.getValue()
    );
    TripTimes tripTimesFailingSubModeMatchingAgency = createTestTripTimesWithSubmode(
      TransmodelTransportSubmode.LOCAL.getValue()
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      false,
      Set.of(),
      List.of(
        TransitFilterRequest
          .of()
          .addSelect(
            SelectRequest.of().withAgencies(List.of(TransitModelForTest.AGENCY.getId())).build()
          )
          .addNot(
            SelectRequest
              .of()
              .withTransportModes(
                List.of(
                  new MainAndSubMode(
                    TransitMode.BUS,
                    SubMode.of(TransmodelTransportSubmode.LOCAL.getValue())
                  )
                )
              )
              .build()
          )
          .build()
      )
    );

    assertTrue(filter.tripTimesPredicate(tripTimesMatchingSubModeMatchingAgency, true));
    assertFalse(filter.tripTimesPredicate(tripTimesFailingSubModeMatchingAgency, true));
  }

  @Test
  void transitModeFilteringTest() {
    TripTimes tripTimes = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
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
  void notFilteringExpectedTripTimesTest() {
    TripTimes tripTimes = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      null
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      false,
      Set.of(),
      filterForMode(TransitMode.BUS)
    );

    boolean valid = filter.tripTimesPredicate(tripTimes, true);

    assertTrue(valid);
  }

  @Test
  void bikesAllowedFilteringTest() {
    TripTimes tripTimes = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      null
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      true,
      false,
      true,
      WheelchairPreferences.DEFAULT,
      false,
      false,
      Set.of(),
      List.of(AllowAllTransitFilter.of())
    );

    boolean valid = filter.tripTimesPredicate(tripTimes, true);

    assertFalse(valid);
  }

  @Test
  void carsAllowedFilteringTest() {
    TripTimes tripTimes = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      null
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      true,
      true,
      WheelchairPreferences.DEFAULT,
      false,
      false,
      Set.of(),
      List.of(AllowAllTransitFilter.of())
    );

    boolean valid = filter.tripTimesPredicate(tripTimes, true);

    assertFalse(valid);
  }

  @Test
  void removeInaccessibleTrip() {
    TripTimes tripTimes = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      null
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      true,
      WheelchairPreferences.DEFAULT,
      false,
      false,
      Set.of(),
      List.of(AllowAllTransitFilter.of())
    );

    boolean valid = filter.tripTimesPredicate(tripTimes, true);

    assertFalse(valid);
  }

  @Test
  void keepAccessibleTrip() {
    TripTimes wheelchairAccessibleTrip = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.PLANNED
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      true,
      WheelchairPreferences.DEFAULT,
      false,
      false,
      Set.of(),
      filterForMode(TransitMode.BUS)
    );

    boolean valid = filter.tripTimesPredicate(wheelchairAccessibleTrip, true);

    assertTrue(valid);
  }

  @Test
  void keepRealTimeAccessibleTrip() {
    RealTimeTripTimes realTimeWheelchairAccessibleTrip = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.PLANNED
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      true,
      WheelchairPreferences.DEFAULT,
      false,
      false,
      Set.of(),
      filterForMode(TransitMode.BUS)
    );

    assertFalse(filter.tripTimesPredicate(realTimeWheelchairAccessibleTrip, true));

    realTimeWheelchairAccessibleTrip.updateWheelchairAccessibility(Accessibility.POSSIBLE);

    assertTrue(filter.tripTimesPredicate(realTimeWheelchairAccessibleTrip, true));
  }

  @Test
  void includePlannedCancellationsTest() {
    TripTimes tripTimesWithCancellation = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.CANCELLATION
    );
    TripTimes tripTimesWithReplaced = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.REPLACED
    );

    // Given
    var filter1 = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      false,
      WheelchairPreferences.DEFAULT,
      true,
      false,
      Set.of(),
      filterForMode(TransitMode.BUS)
    );

    // When
    boolean valid1 = filter1.tripTimesPredicate(tripTimesWithCancellation, true);
    // Then
    assertTrue(valid1);

    // When
    boolean valid2 = filter1.tripTimesPredicate(tripTimesWithReplaced, true);
    // Then
    assertTrue(valid2);

    // Given
    var filter2 = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      false,
      Set.of(),
      List.of(AllowAllTransitFilter.of())
    );

    // When
    boolean valid3 = filter2.tripTimesPredicate(tripTimesWithCancellation, true);
    // Then
    assertFalse(valid3);

    // When
    boolean valid4 = filter2.tripTimesPredicate(tripTimesWithReplaced, true);
    // Then
    assertFalse(valid4);
  }

  @Test
  void includeRealtimeCancellationsTest() {
    TripTimes tripTimes = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.PLANNED
    );

    RealTimeTripTimes tripTimesWithCancellation = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.PLANNED
    );
    tripTimesWithCancellation.cancelTrip();

    // Given
    var filter1 = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      false,
      WheelchairPreferences.DEFAULT,
      false,
      true,
      Set.of(),
      filterForMode(TransitMode.BUS)
    );

    // When
    boolean valid1 = filter1.tripTimesPredicate(tripTimes, true);
    // Then
    assertTrue(valid1);

    // When
    boolean valid2 = filter1.tripTimesPredicate(tripTimesWithCancellation, true);
    // Then
    assertTrue(valid2);

    // Given
    var filter2 = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      false,
      Set.of(),
      List.of(AllowAllTransitFilter.of())
    );

    // When
    boolean valid3 = filter2.tripTimesPredicate(tripTimes, true);
    // Then
    assertTrue(valid3);

    // When
    boolean valid4 = filter2.tripTimesPredicate(tripTimesWithCancellation, true);
    // Then
    assertFalse(valid4);
  }

  @Test
  void testBikesAllowed() {
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
  void testCarsAllowed() {
    RouteBuilder routeBuilder = TransitModelForTest.route("1");
    TripBuilder trip = Trip.of(TransitModelForTest.id("T1")).withRoute(routeBuilder.build());

    assertEquals(
      CarAccess.UNKNOWN,
      RouteRequestTransitDataProviderFilter.carAccessForTrip(trip.build())
    );
    trip.withCarsAllowed(CarAccess.ALLOWED);
    assertEquals(
      CarAccess.ALLOWED,
      RouteRequestTransitDataProviderFilter.carAccessForTrip(trip.build())
    );
    trip.withCarsAllowed(CarAccess.NOT_ALLOWED);
    assertEquals(
      CarAccess.NOT_ALLOWED,
      RouteRequestTransitDataProviderFilter.carAccessForTrip(trip.build())
    );
  }

  @Test
  void multipleFilteringTest() {
    TripTimes matchingTripTimes = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.PLANNED
    );
    TripTimes failingTripTimes1 = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.RAIL,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.PLANNED
    );
    TripTimes failingTripTimes2 = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.RAIL,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.CANCELLATION
    );
    TripTimes failingTripTimes3 = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.RAIL,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.CANCELLATION
    );
    TripTimes failingTripTimes4 = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.PLANNED
    );
    TripTimes failingTripTimes5 = createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.CANCELLATION
    );

    var filter = new RouteRequestTransitDataProviderFilter(
      true,
      false,
      true,
      DEFAULT_ACCESSIBILITY,
      false,
      false,
      Set.of(),
      filterForMode(TransitMode.BUS)
    );

    assertTrue(filter.tripTimesPredicate(matchingTripTimes, true));

    assertFalse(filter.tripTimesPredicate(failingTripTimes1, true));
    assertFalse(filter.tripTimesPredicate(failingTripTimes2, true));
    assertFalse(filter.tripTimesPredicate(failingTripTimes3, true));
    assertFalse(filter.tripTimesPredicate(failingTripTimes4, true));
    assertFalse(filter.tripTimesPredicate(failingTripTimes5, true));
  }

  private boolean validateModesOnTripTimes(
    Collection<MainAndSubMode> allowedModes,
    TripTimes tripTimes
  ) {
    var filter = new RouteRequestTransitDataProviderFilter(
      false,
      false,
      false,
      DEFAULT_ACCESSIBILITY,
      false,
      false,
      Set.of(),
      filterForModes(allowedModes)
    );

    return filter.tripTimesPredicate(tripTimes, true);
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

    TripTimes tripTimes = TripTimesFactory.tripTimes(
      TransitModelForTest.trip("1").withRoute(route).build(),
      List.of(new StopTime()),
      new Deduplicator()
    );

    return new TripPatternForDate(tripPattern, List.of(tripTimes), List.of(), LocalDate.now());
  }

  private List<TransitFilter> filterForMode(TransitMode mode) {
    return filterForModes(List.of(new MainAndSubMode(mode)));
  }

  private List<TransitFilter> filterForModes(Collection<MainAndSubMode> modes) {
    return List.of(
      TransitFilterRequest
        .of()
        .addSelect(SelectRequest.of().withTransportModes(List.copyOf(modes)).build())
        .build()
    );
  }

  private List<TransitFilter> filterForModesAndFilterForBannedAgencies(
    List<MainAndSubMode> modes,
    List<FeedScopedId> agencyIds
  ) {
    return List.of(
      TransitFilterRequest
        .of()
        .addSelect(SelectRequest.of().withTransportModes(List.copyOf(modes)).build())
        .build(),
      TransitFilterRequest.of().addNot(SelectRequest.of().withAgencies(agencyIds).build()).build()
    );
  }

  private List<TransitFilter> combinedFilterForModesAndBannedAgencies(
    List<MainAndSubMode> modes,
    List<FeedScopedId> agencyIds
  ) {
    return List.of(
      TransitFilterRequest
        .of()
        .addSelect(SelectRequest.of().withTransportModes(List.copyOf(modes)).build())
        .addNot(SelectRequest.of().withAgencies(agencyIds).build())
        .build()
    );
  }

  private RealTimeTripTimes createTestTripTimes(
    FeedScopedId tripId,
    Route route,
    BikeAccess bikeAccess,
    CarAccess carAccess,
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
      .withCarsAllowed(carAccess)
      .withWheelchairBoarding(wheelchairBoarding)
      .withNetexAlteration(tripAlteration)
      .build();

    StopTime stopTime = new StopTime();
    stopTime.setStop(STOP_FOR_TEST);
    stopTime.setArrivalTime(60);
    stopTime.setDepartureTime(60);
    stopTime.setStopSequence(0);

    return TripTimesFactory.tripTimes(trip, List.of(stopTime), new Deduplicator());
  }

  private TripTimes createTestTripTimesWithSubmode(String submode) {
    return createTestTripTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      submode,
      Accessibility.NOT_POSSIBLE,
      null
    );
  }

  public static RegularStop stopForTest(
    String idAndName,
    Accessibility wheelchair,
    double lat,
    double lon
  ) {
    return TEST_MODEL
      .stop(idAndName)
      .withCoordinate(new WgsCoordinate(lat, lon))
      .withWheelchairAccessibility(wheelchair)
      .build();
  }

  private static StopTime getStopTime(String idAndName, PickDrop scheduled) {
    var stopTime1 = new StopTime();
    stopTime1.setStop(TEST_MODEL.stop(idAndName, 0.0, 0.0).build());
    stopTime1.setDropOffType(scheduled);
    stopTime1.setPickupType(scheduled);
    return stopTime1;
  }
}
