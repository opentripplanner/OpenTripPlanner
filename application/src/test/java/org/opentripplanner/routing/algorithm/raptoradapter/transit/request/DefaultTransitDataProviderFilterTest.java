package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
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
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.transit.model.timetable.TripBuilder;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;

class DefaultTransitDataProviderFilterTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final Route ROUTE = TimetableRepositoryForTest.route("1").build();

  private static final FeedScopedId TRIP_ID = TimetableRepositoryForTest.id("T1");

  private static final RegularStop STOP_FOR_TEST = TEST_MODEL.stop("TEST:STOP", 0, 0).build();

  private static final WheelchairPreferences DEFAULT_ACCESSIBILITY = WheelchairPreferences.DEFAULT;

  private static final AccessibilityPreferences RELAXED_ACCESSIBILITY_PREFERENCE =
    AccessibilityPreferences.ofCost(0, 10);
  private static final WheelchairPreferences RELAXED_ACCESSIBILITY = WheelchairPreferences.of()
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
    var tripPattern = TripPattern.of(TimetableRepositoryForTest.id("P1"))
      .withRoute(TimetableRepositoryForTest.route("1").build())
      .withStopPattern(stopPattern)
      .build()
      .getRoutingTripPattern();

    var request = RouteRequest.defaultValue()
      .copyOf()
      .withFrom(GenericLocation.fromCoordinate(0, 0))
      .withTo(GenericLocation.fromCoordinate(0, 0))
      .withPreferences(builder -> builder.withWheelchair(accessibility))
      .withJourney(builder -> builder.withWheelchair(true))
      .buildRequest();

    var filter = DefaultTransitDataProviderFilter.ofRequest(request);

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
    var tripPattern = TripPattern.of(TimetableRepositoryForTest.id("P1"))
      .withRoute(TimetableRepositoryForTest.route("1").build())
      .withStopPattern(stopPattern)
      .build()
      .getRoutingTripPattern();

    var filter = DefaultTransitDataProviderFilter.of()
      .withIncludeRealtimeCancellations(includeRealtimeCancellations)
      .build();

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
  void emptyRequestFiltersShouldDiscardEverything() {
    var request = RouteRequest.defaultValue()
      .copyOf()
      .withFrom(GenericLocation.fromCoordinate(0, 0))
      .withTo(GenericLocation.fromCoordinate(0, 0))
      .withJourney(journey -> journey.withTransit(transit -> transit.withFilters(List.of())))
      .buildRequest();

    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();
    var filter = DefaultTransitDataProviderFilterBuilder.ofRequest(request).build();

    assertNull(filter.createTripFilter(tripPatternForDate.getTripPattern().getPattern()));
  }

  @Test
  void notFilteringExpectedTripPatternForDateTest() {
    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();

    var filter = DefaultTransitDataProviderFilter.of()
      .withFilters(filterForMode(TransitMode.BUS))
      .build();

    boolean valid =
      filter.createTripFilter(tripPatternForDate.getTripPattern().getPattern()) != null;

    assertTrue(valid);
  }

  @Test
  void bannedRouteFilteringTest() {
    TripPatternForDate tripPatternForDate = createTestTripPatternForDate();

    var filter = DefaultTransitDataProviderFilter.of()
      .addFilter(
        TransitFilterRequest.of()
          .addNot(SelectRequest.of().withRoutes(List.of(ROUTE.getId())).build())
          .build()
      )
      .build();

    boolean valid =
      filter.createTripFilter(tripPatternForDate.getTripPattern().getPattern()) != null;

    assertFalse(valid);
  }

  @Test
  void bannedTripFilteringTest() {
    var patternAndTimes = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      null
    );

    var filter = DefaultTransitDataProviderFilter.of().withBannedTrips(Set.of(TRIP_ID)).build();

    assertFalse(validate(filter, patternAndTimes));
  }

  /**
   * Filtering trips with 2 filters: a selection by submode filter and an exclusion by agency filter.
   * A trip matches if it matches either of them.
   */
  @Test
  void matchModeFilterAndBannedAgencyFilter() {
    var ptMatchingSubModeMatchingAgency = createPatternAndTimesWithSubmode(
      TransmodelTransportSubmode.UNKNOWN.getValue()
    );
    var ptFailingSubModeMatchingAgency = createPatternAndTimesWithSubmode(
      TransmodelTransportSubmode.LOCAL.getValue()
    );

    var filter = DefaultTransitDataProviderFilter.of()
      .withFilters(
        filterForModesAndFilterForBannedAgencies(
          List.of(
            new MainAndSubMode(
              TransitMode.BUS,
              SubMode.of(TransmodelTransportSubmode.UNKNOWN.getValue())
            )
          ),
          List.of(TimetableRepositoryForTest.OTHER_AGENCY.getId())
        )
      )
      .build();

    assertTrue(validate(filter, ptMatchingSubModeMatchingAgency));
    assertTrue(validate(filter, ptFailingSubModeMatchingAgency));
  }

  /**
   * Filtering trips with one filter combining selection by submode amd exclusion by agency:
   * a trip matches if it matches both of them.
   */
  @Test
  void matchCombinedModesAndBannedAgencyFilter() {
    var ptMatchingSubModeMatchingAgency = createPatternAndTimesWithSubmode(
      TransmodelTransportSubmode.UNKNOWN.getValue()
    );
    var ptFailingSubModeMatchingAgency = createPatternAndTimesWithSubmode(
      TransmodelTransportSubmode.LOCAL.getValue()
    );

    var filter = DefaultTransitDataProviderFilter.of()
      .withFilters(
        combinedFilterForModesAndBannedAgencies(
          List.of(
            new MainAndSubMode(
              TransitMode.BUS,
              SubMode.of(TransmodelTransportSubmode.UNKNOWN.getValue())
            )
          ),
          List.of(TimetableRepositoryForTest.OTHER_AGENCY.getId())
        )
      )
      .build();

    assertTrue(validate(filter, ptMatchingSubModeMatchingAgency));
    assertFalse(validate(filter, ptFailingSubModeMatchingAgency));
  }

  @Test
  void matchSelectedAgencyExcludedSubMode() {
    var ptMatchingSubModeMatchingAgency = createPatternAndTimesWithSubmode(
      TransmodelTransportSubmode.UNKNOWN.getValue()
    );
    var ptFailingSubModeMatchingAgency = createPatternAndTimesWithSubmode(
      TransmodelTransportSubmode.LOCAL.getValue()
    );

    var filter = DefaultTransitDataProviderFilter.of()
      .withFilters(
        List.of(
          TransitFilterRequest.of()
            .addSelect(
              SelectRequest.of()
                .withAgencies(List.of(TimetableRepositoryForTest.AGENCY.getId()))
                .build()
            )
            .addNot(
              SelectRequest.of()
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
      )
      .build();

    assertTrue(validate(filter, ptMatchingSubModeMatchingAgency));
    assertFalse(validate(filter, ptFailingSubModeMatchingAgency));
  }

  @Test
  void transitModeFilteringTest() {
    var patternTimes = createPatternAndTimes(
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

    assertFalse(validateModes(List.of(new MainAndSubMode(BUS, REGIONAL_BUS)), patternTimes));
    assertFalse(validateModes(List.of(new MainAndSubMode(RAIL, LOCAL_BUS)), patternTimes));

    assertTrue(validateModes(List.of(new MainAndSubMode(BUS)), patternTimes));
    assertTrue(validateModes(List.of(new MainAndSubMode(BUS, LOCAL_BUS)), patternTimes));
  }

  @Ignore
  void selectCombinationTest() {
    // This test illustrates a bug in the filtering logic.
    // We have a filter that corresponds to this boolean expression:
    //   (agency == AGENCY && mode == BUS) || (agency == OTHER_AGENCY && mode == RAIL)
    // This results in a match with a trip { agency: OTHER_AGENCY, mode: BUS } even though it
    // shouldn't.
    var transitFilter = TransitFilterRequest.of()
      .addSelect(
        SelectRequest.of()
          .withAgencies(List.of(TimetableRepositoryForTest.OTHER_AGENCY.getId()))
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS)))
          .build()
      )
      .addSelect(
        SelectRequest.of()
          .withAgencies(List.of(TimetableRepositoryForTest.AGENCY.getId()))
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.RAIL)))
          .build()
      )
      .build();

    var filter = DefaultTransitDataProviderFilter.of().addFilter(transitFilter).build();

    var patternTimes = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      "localBus",
      Accessibility.NOT_POSSIBLE,
      null,
      true
    );

    assertFalse(validate(filter, patternTimes));
  }

  @Test
  void notFilteringExpectedTripTimesTest() {
    var pt = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      null
    );

    var filter = DefaultTransitDataProviderFilter.of()
      .withFilters(filterForMode(TransitMode.BUS))
      .build();

    boolean valid = validate(filter, pt);

    assertTrue(valid);
  }

  @Test
  void bikesAllowedFilteringTest() {
    var patternTimes = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      null
    );

    var filter = DefaultTransitDataProviderFilter.of().withRequireBikesAllowed(true).build();

    boolean valid = validate(filter, patternTimes);

    assertFalse(valid);
  }

  @Test
  void carsAllowedFilteringTest() {
    var patternTimes = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      null
    );

    var filter = DefaultTransitDataProviderFilter.of().withRequireCarsAllowed(true).build();

    boolean valid = validate(filter, patternTimes);

    assertFalse(valid);
  }

  @Test
  void removeInaccessibleTrip() {
    var patternTimes = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      null
    );

    var filter = DefaultTransitDataProviderFilter.of()
      .withRequireWheelchairAccessibleTrips(true)
      .build();

    boolean valid = validate(filter, patternTimes);

    assertFalse(valid);
  }

  @Test
  void keepAccessibleTrip() {
    var wheelchairAccessibleTrip = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.PLANNED
    );

    var filter = DefaultTransitDataProviderFilter.of()
      .withRequireWheelchairAccessibleTrips(true)
      .build();

    boolean valid = validate(filter, wheelchairAccessibleTrip);

    assertTrue(valid);
  }

  @Test
  void keepRealTimeAccessibleTrip() {
    var patternAndTimes = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.PLANNED
    );
    var builder = patternAndTimes.tripTimes().createRealTimeFromScheduledTimes();

    var filter = DefaultTransitDataProviderFilter.of()
      .withRequireWheelchairAccessibleTrips(true)
      .build();

    assertFalse(validate(filter, patternAndTimes.withTimes(builder.build())));

    builder.withWheelchairAccessibility(Accessibility.POSSIBLE);

    assertTrue(validate(filter, patternAndTimes.withTimes(builder.build())));
  }

  @Test
  void includePlannedCancellationsTest() {
    var patternTimesWithCancellation = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.CANCELLATION
    );
    var patternTimesWithReplaced = createPatternAndTimes(
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
    var filter1 = DefaultTransitDataProviderFilter.of()
      .withIncludePlannedCancellations(true)
      .build();

    // When
    boolean valid1 = validate(filter1, patternTimesWithCancellation);
    // Then
    assertTrue(valid1);

    // When
    boolean valid2 = validate(filter1, patternTimesWithReplaced);
    // Then
    assertTrue(valid2);

    // Given
    var filter2 = DefaultTransitDataProviderFilter.of()
      .withIncludePlannedCancellations(false)
      .build();

    // When
    boolean valid3 = validate(filter2, patternTimesWithCancellation);
    // Then
    assertFalse(valid3);

    // When
    boolean valid4 = validate(filter2, patternTimesWithReplaced);
    // Then
    assertFalse(valid4);
  }

  @Test
  void includeRealtimeCancellationsTest() {
    var patternTimes = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.PLANNED
    );

    var cancelled = patternTimes
      .tripTimes()
      .createRealTimeFromScheduledTimes()
      .cancelTrip()
      .build();

    var patternTimesWithCancellation = patternTimes.withTimes(cancelled);

    // Given
    var filter1 = DefaultTransitDataProviderFilter.of()
      .withIncludeRealtimeCancellations(true)
      .build();

    // When
    boolean valid1 = validate(filter1, patternTimes);
    // Then
    assertTrue(valid1);

    // When
    boolean valid2 = validate(filter1, patternTimesWithCancellation);
    // Then
    assertTrue(valid2);

    // Given
    var filter2 = DefaultTransitDataProviderFilter.of()
      .withIncludeRealtimeCancellations(false)
      .build();

    // When
    boolean valid3 = validate(filter2, patternTimes);
    // Then
    assertTrue(valid3);

    // When
    boolean valid4 = validate(filter2, patternTimesWithCancellation);
    // Then
    assertFalse(valid4);
  }

  @Test
  void testBikesAllowed() {
    RouteBuilder routeBuilder = TimetableRepositoryForTest.route("1");
    TripBuilder trip = Trip.of(TimetableRepositoryForTest.id("T1")).withRoute(routeBuilder.build());

    assertEquals(
      BikeAccess.UNKNOWN,
      DefaultTransitDataProviderFilter.bikeAccessForTrip(trip.build())
    );
    trip.withBikesAllowed(BikeAccess.ALLOWED);
    assertEquals(
      BikeAccess.ALLOWED,
      DefaultTransitDataProviderFilter.bikeAccessForTrip(trip.build())
    );
    trip.withBikesAllowed(BikeAccess.NOT_ALLOWED);
    assertEquals(
      BikeAccess.NOT_ALLOWED,
      DefaultTransitDataProviderFilter.bikeAccessForTrip(trip.build())
    );
    trip.withRoute(routeBuilder.withBikesAllowed(BikeAccess.ALLOWED).build());
    assertEquals(
      BikeAccess.NOT_ALLOWED,
      DefaultTransitDataProviderFilter.bikeAccessForTrip(trip.build())
    );
    trip.withBikesAllowed(BikeAccess.UNKNOWN);
    assertEquals(
      BikeAccess.ALLOWED,
      DefaultTransitDataProviderFilter.bikeAccessForTrip(trip.build())
    );
    trip.withRoute(routeBuilder.withBikesAllowed(BikeAccess.NOT_ALLOWED).build());
    assertEquals(
      BikeAccess.NOT_ALLOWED,
      DefaultTransitDataProviderFilter.bikeAccessForTrip(trip.build())
    );
  }

  @Test
  void testCarsAllowed() {
    var patternTimesCarsAllowed = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.UNKNOWN,
      CarAccess.ALLOWED,
      TransitMode.FERRY,
      null,
      Accessibility.NO_INFORMATION,
      TripAlteration.PLANNED
    );

    var patternTimesCarsNotAllowed = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.UNKNOWN,
      CarAccess.NOT_ALLOWED,
      TransitMode.FERRY,
      null,
      Accessibility.NO_INFORMATION,
      TripAlteration.PLANNED
    );

    var patternTimesCarsUnknown = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.UNKNOWN,
      CarAccess.UNKNOWN,
      TransitMode.FERRY,
      null,
      Accessibility.NO_INFORMATION,
      TripAlteration.PLANNED
    );

    DefaultTransitDataProviderFilter filter = DefaultTransitDataProviderFilter.of()
      .withRequireCarsAllowed(true)
      .build();

    assertTrue(validate(filter, patternTimesCarsAllowed));
    assertFalse(validate(filter, patternTimesCarsNotAllowed));
    assertFalse(validate(filter, patternTimesCarsUnknown));
  }

  @Test
  void multipleFilteringTest() {
    var matchingPatternTimes = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.PLANNED
    );
    var failingPatternTimes1 = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.RAIL,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.PLANNED
    );
    var failingPatternTimes2 = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.RAIL,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.CANCELLATION
    );
    var failingPatternTimes3 = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.RAIL,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.CANCELLATION
    );
    var failingPatternTimes4 = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.NOT_POSSIBLE,
      TripAlteration.PLANNED
    );
    var failingPatternTimes5 = createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      null,
      Accessibility.POSSIBLE,
      TripAlteration.CANCELLATION
    );

    var filter = DefaultTransitDataProviderFilter.of()
      .withRequireBikesAllowed(true)
      .withRequireWheelchairAccessibleTrips(true)
      .withFilters(filterForMode(TransitMode.BUS))
      .build();

    assertTrue(validate(filter, matchingPatternTimes));

    assertFalse(validate(filter, failingPatternTimes1));
    assertFalse(validate(filter, failingPatternTimes2));
    assertFalse(validate(filter, failingPatternTimes3));
    assertFalse(validate(filter, failingPatternTimes4));
    assertFalse(validate(filter, failingPatternTimes5));
  }

  private boolean validateModes(
    Collection<MainAndSubMode> allowedModes,
    PatternAndTimes patternAndTimes
  ) {
    var filter = DefaultTransitDataProviderFilter.of()
      .withFilters(filterForModes(allowedModes))
      .build();

    var timesFilter = filter.createTripFilter(patternAndTimes.pattern());
    if (timesFilter == null) {
      return false;
    }
    return timesFilter.test(patternAndTimes.tripTimes());
  }

  private TripPatternForDate createTestTripPatternForDate() {
    Route route = TimetableRepositoryForTest.route("1").build();

    var stopTime = new StopTime();
    stopTime.setStop(STOP_FOR_TEST);
    StopPattern stopPattern = new StopPattern(List.of(stopTime));
    RoutingTripPattern tripPattern = TripPattern.of(TimetableRepositoryForTest.id("P1"))
      .withRoute(route)
      .withStopPattern(stopPattern)
      .build()
      .getRoutingTripPattern();

    TripTimes tripTimes = TripTimesFactory.tripTimes(
      TimetableRepositoryForTest.trip("1").withRoute(route).build(),
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
      TransitFilterRequest.of()
        .addSelect(SelectRequest.of().withTransportModes(List.copyOf(modes)).build())
        .build()
    );
  }

  private List<TransitFilter> filterForModesAndFilterForBannedAgencies(
    List<MainAndSubMode> modes,
    List<FeedScopedId> agencyIds
  ) {
    return List.of(
      TransitFilterRequest.of()
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
      TransitFilterRequest.of()
        .addSelect(SelectRequest.of().withTransportModes(List.copyOf(modes)).build())
        .addNot(SelectRequest.of().withAgencies(agencyIds).build())
        .build()
    );
  }

  private record PatternAndTimes(TripPattern pattern, TripTimes tripTimes) {
    public PatternAndTimes withTimes(TripTimes tripTimes) {
      return new PatternAndTimes(pattern, tripTimes);
    }
  }

  private PatternAndTimes createPatternAndTimes(
    FeedScopedId tripId,
    Route route,
    BikeAccess bikeAccess,
    CarAccess carAccess,
    TransitMode mode,
    String submode,
    Accessibility wheelchairBoarding,
    TripAlteration tripAlteration
  ) {
    return createPatternAndTimes(
      tripId,
      route,
      bikeAccess,
      carAccess,
      mode,
      submode,
      wheelchairBoarding,
      tripAlteration,
      false
    );
  }

  private PatternAndTimes createPatternAndTimes(
    FeedScopedId tripId,
    Route route,
    BikeAccess bikeAccess,
    CarAccess carAccess,
    TransitMode mode,
    String submode,
    Accessibility wheelchairBoarding,
    TripAlteration tripAlteration,
    boolean multipleSubmodes
  ) {
    Trip trip = Trip.of(tripId)
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
    stopTime.setStop(STOP_FOR_TEST);

    StopPattern stopPattern = new StopPattern(List.of(stopTime));
    var tripPattern = TripPattern.of(TimetableRepositoryForTest.id("P1"))
      .withRoute(route)
      .withStopPattern(stopPattern)
      .withMode(mode)
      .withNetexSubmode(SubMode.of(submode))
      .withContainsMultipleModes(multipleSubmodes)
      .build();

    TripTimes tripTimes = TripTimesFactory.tripTimes(
      trip,
      List.of(new StopTime()),
      new Deduplicator()
    );

    return new PatternAndTimes(tripPattern, tripTimes);
  }

  private PatternAndTimes createPatternAndTimesWithSubmode(String submode) {
    return createPatternAndTimes(
      TRIP_ID,
      ROUTE,
      BikeAccess.NOT_ALLOWED,
      CarAccess.NOT_ALLOWED,
      TransitMode.BUS,
      submode,
      Accessibility.NOT_POSSIBLE,
      null,
      true
    );
  }

  public static RegularStop stopForTest(
    String idAndName,
    Accessibility wheelchair,
    double lat,
    double lon
  ) {
    return TEST_MODEL.stop(idAndName)
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

  private boolean validate(
    DefaultTransitDataProviderFilter filter,
    PatternAndTimes patternAndTimes
  ) {
    var tripTimesFilter = filter.createTripFilter(patternAndTimes.pattern());
    if (tripTimesFilter == null) {
      return false;
    }
    return tripTimesFilter.test(patternAndTimes.tripTimes());
  }
}
