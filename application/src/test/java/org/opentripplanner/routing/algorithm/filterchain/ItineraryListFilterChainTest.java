package org.opentripplanner.routing.algorithm.filterchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_ARRIVAL_TIME;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_DEPARTURE_TIME;
import static org.opentripplanner.model.plan.TestItineraryBuilder.BUS_ROUTE;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newTime;
import static org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile.ofDebugEnabled;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.algorithm.filterchain.api.GroupBySimilarity;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.utils.lang.Box;

/**
 * This class test the whole filter chain with a few test cases. Each filter should be tested with a
 * unit test. This is just a some test on top of the other filter unit-tests.
 */
class ItineraryListFilterChainTest implements PlanTestConstants {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final Place A = Place.forStop(TEST_MODEL.stop("A").build());
  private static final Place B = Place.forStop(TEST_MODEL.stop("B").build());
  private static final Place C = Place.forStop(TEST_MODEL.stop("C").build());
  private static final Place D = Place.forStop(TEST_MODEL.stop("D").build());
  private static final Place E = Place.forStop(TEST_MODEL.stop("E").build());

  private static final int I3_LATE_START_TIME = T11_33;
  private static final Duration SW_D10m = Duration.ofSeconds(D10m);

  private Itinerary i1;
  private Itinerary i2;
  private Itinerary i3;

  @BeforeEach
  void setUpItineraries() {
    // Add some itineraries, with some none optimal options
    // Short walk - 2 minutes - to destination:
    i1 = newItinerary(A, T11_06).walk(D2m, E).build();

    // Not optimal, takes longer than walking
    i2 = newItinerary(A).bus(21, T11_06, T11_09, E).build();

    // Not optimal, departure is very late
    i3 = newItinerary(A).bus(20, I3_LATE_START_TIME, I3_LATE_START_TIME + D1m, E).build();
  }

  @Test
  void testDefaultFilterChain() {
    // Given a default chain
    ItineraryListFilterChain chain = createBuilder(false, false, 10).build();

    assertEquals(toStr(List.of(i1, i3)), toStr(chain.filter(List.of(i1, i2, i3))));
  }

  @Test
  void testFilterChainWithSearchWindowFilterSet() {
    ItineraryListFilterChain chain = createBuilder(false, false, 10)
      .withSearchWindow(TestItineraryBuilder.newTime(T11_00).toInstant(), SW_D10m)
      .build();
    var result = chain.filter(List.of(i1, i2, i3));
    assertEquals(toStr(List.of(i1)), toStr(result));
  }

  @Test
  void withMinBikeParkingDistance() {
    // Given a "default" chain
    ItineraryListFilterChain chain = createBuilder(false, false, 10)
      .withMinBikeParkingDistance(500)
      .build();

    var shortBikeToStop = newItinerary(A)
      .bicycle(T11_05, T11_06, B)
      .rail(30, T11_16, T11_20, C)
      .build();
    assertEquals(300, shortBikeToStop.legs().get(0).getDistanceMeters());
    // should do nothing to non-bike trips but remove a bike+ride route that cycles only for one minute
    assertEquals(List.of(i1), chain.filter(List.of(i1, shortBikeToStop)));
  }

  @Test
  void testDebugFilterChain() {
    // Given a filter-chain with debugging enabled
    ItineraryListFilterChain chain = createBuilder(false, true, 3)
      .withSearchWindow(newTime(T11_00).toInstant(), SW_D10m)
      .build();

    // Walk first, then transit sorted on arrival-time
    assertEquals(toStr(List.of(i1, i2, i3)), toStr(chain.filter(List.of(i1, i2, i3))));
    assertEquals("[]", toStringOfTags(i1.systemNotices()));
    assertEquals(
      "[transit-vs-street-filter, transit-vs-walk-filter]",
      toStringOfTags(i2.systemNotices())
    );
    assertEquals("[outside-search-window]", toStringOfTags(i3.systemNotices()));
  }

  @Test
  void removeAllWalkingOnly() {
    ItineraryListFilterChain chain = createBuilder(false, false, 20)
      .withRemoveWalkAllTheWayResults(true)
      .build();

    Itinerary walk = newItinerary(A, T11_06).walk(D10m, E).build();
    Itinerary bus = newItinerary(A).bus(21, T11_06, T11_12, E).build();

    assertEquals(toStr(List.of(bus)), toStr(chain.filter(List.of(walk, bus))));
  }

  @Test
  void groupByTheLongestItineraryAndTwoGroups() {
    ItineraryListFilterChain chain = createBuilder(false, false, 20)
      .addGroupBySimilarity(GroupBySimilarity.createWithOneItineraryPerGroup(.5))
      .build();

    // Group 1
    Itinerary i1 = newItinerary(A, 6).walk(240, C).build();

    // Group 2, with 2 itineraries
    Itinerary i2 = newItinerary(A).bus(1, 0, 50, B).bus(11, 52, 100, C).build();
    Itinerary i3 = newItinerary(A).bus(1, 0, 50, B).bus(12, 55, 102, C).build();

    List<Itinerary> input = List.of(i1, i2, i3);

    // With min Limit = 1, expect the best trips from both groups
    chain.filter(input);

    assertFalse(i1.isFlaggedForDeletion());
    assertFalse(i2.isFlaggedForDeletion());
    assertTrue(i3.isFlaggedForDeletion());
  }

  @Test
  void testSameFirstOrLastTripFilter() {
    ItineraryListFilterChain chain = createBuilder(false, false, 20)
      .withSameFirstOrLastTripFilter(true)
      .build();

    int ID_1 = 1;
    int ID_2 = 2;
    int ID_3 = 3;

    Itinerary i1 = newItinerary(A).bus(ID_1, 0, 50, B).bus(ID_2, 52, 100, C).build();
    Itinerary i2 = newItinerary(A).bus(ID_1, 0, 50, B).bus(ID_3, 52, 150, C).build();

    List<Itinerary> input = List.of(i1, i2);

    chain.filter(input);

    assertFalse(i1.isFlaggedForDeletion());
    assertTrue(i2.isFlaggedForDeletion());
  }

  @Test
  void testRoutingErrorsOriginDestinationTooCloseTest() {
    ItineraryListFilterChain chain = createBuilder(false, false, 20)
      .withRemoveWalkAllTheWayResults(true)
      .withRemoveTransitWithHigherCostThanBestOnStreetOnly(
        CostLinearFunction.of(Duration.ofSeconds(0), 1.0)
      )
      .build();

    Itinerary walk = newItinerary(A, T11_06).walk(D10m, E).build();
    Itinerary bus = newItinerary(A).bus(21, T11_06, T11_28, E).build();

    assertTrue(chain.filter(List.of(walk, bus)).isEmpty());

    final List<RoutingError> routingErrors = chain.getRoutingErrors();
    assertEquals(1, routingErrors.size());
    assertEquals(RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT, routingErrors.get(0).code);
  }

  @Test
  void routingErrorsOutsideWindowTest() {
    var chain = createBuilder(false, false, 20)
      .withRemoveWalkAllTheWayResults(true)
      .withSearchWindow(Instant.from(newTime(T11_00)), Duration.ofMinutes(5))
      .build();

    Itinerary bus = newItinerary(A).bus(21, T11_06, T11_23, E).build();

    assertTrue(chain.filter(List.of(bus)).isEmpty());

    final List<RoutingError> routingErrors = chain.getRoutingErrors();
    assertEquals(1, routingErrors.size());
    assertEquals(
      RoutingErrorCode.NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW,
      routingErrors.get(0).code
    );
  }

  @Test
  void transitAlertsTest() {
    var transitAlertService = Mockito.mock(TransitAlertService.class);

    // Given a chain with transit alerts
    var chain = createBuilder(false, false, 20)
      .withTransitAlerts(transitAlertService, ignore -> null)
      .build();

    // When running it with transit itineraries
    chain.filter(List.of(i1, i2, i3));

    // Then transitAlertService should have been called with stop and route ids
    Mockito.verify(transitAlertService, Mockito.atLeastOnce()).getStopAlerts(
      A.stop.getId(),
      StopCondition.FIRST_DEPARTURE
    );
    Mockito.verify(transitAlertService, Mockito.atLeastOnce()).getStopAlerts(
      E.stop.getId(),
      StopCondition.ARRIVING
    );
    Mockito.verify(transitAlertService, Mockito.atLeastOnce()).getRouteAlerts(BUS_ROUTE.getId());
  }

  @Test
  void removeItinerariesWithSameRoutesAndStops() {
    var i1 = newItinerary(A).bus(21, T11_06, T11_28, E).bus(41, T11_30, T11_32, D).build();
    var i2 = newItinerary(A).bus(22, T11_09, T11_30, E).bus(42, T11_32, T11_33, D).build();
    var i3 = newItinerary(A).bus(23, T11_10, T11_32, E).bus(43, T11_33, T11_50, D).build();

    var i4 = newItinerary(A).bus(31, T11_09, T11_20, D).build();
    var i5 = newItinerary(A).bus(32, T11_10, T11_23, D).build();
    var i6 = newItinerary(A).bus(32, T11_10, T11_23, D).build();

    ItineraryListFilterChain chain = createBuilder(false, false, 10)
      // we need to add the group-by-distance-and-id filter because it undeletes those with the
      // fewest transfers and we want to make sure that the filter under test comes _after_
      .addGroupBySimilarity(GroupBySimilarity.createWithOneItineraryPerGroup(.5))
      .withRemoveTimeshiftedItinerariesWithSameRoutesAndStops(true)
      .build();
    assertEquals(toStr(List.of(i4, i1)), toStr(chain.filter(List.of(i1, i2, i3, i4, i5, i6))));
  }

  private ItineraryListFilterChainBuilder createBuilder(
    boolean arriveBy,
    boolean debug,
    int numOfItineraries
  ) {
    var sortOrder = arriveBy ? STREET_AND_DEPARTURE_TIME : STREET_AND_ARRIVAL_TIME;
    return new ItineraryListFilterChainBuilder(sortOrder)
      .withMaxNumberOfItineraries(numOfItineraries)
      .withRemoveTransitWithHigherCostThanBestOnStreetOnly(
        CostLinearFunction.of(Duration.ofSeconds(0), 1.0)
      )
      .withDebugEnabled(ofDebugEnabled(debug));
  }

  @Test
  void makeSureEmissionDecoratorIsAddedToTheFilterChainTest() {
    final Box<String> state = Box.of("I");
    ItineraryDecorator emissionDecorator = it -> {
      state.modify(v -> v + "+C");
      return it;
    };
    createBuilder(false, false, 10)
      .withEmissions(emissionDecorator)
      .build()
      .filter(List.of(i1, i2));
    assertEquals("I+C+C", state.get());
  }

  @Nested
  class MaxItinerariesBuilderTest {

    @BeforeEach
    void setUpItineraries() {
      i1 = newItinerary(A).bus(21, T11_05, T11_10, E).build();
      i2 = newItinerary(A).bus(31, T11_07, T11_12, E).build();
      i3 = newItinerary(A).bus(41, T11_09, T11_14, E).build();
    }

    @Test
    void testPostProcessorWithMaxItinerariesFilterSetToTwo() {
      // Given a default postProcessor with 'numOfItineraries=2'
      ItineraryListFilterChain chain = createBuilder(false, false, 2).build();
      assertEquals(List.of(i1, i2), chain.filter(List.of(i1, i2, i3)));
    }

    @Test
    void testPostProcessorWithMaxItinerariesFilterSetToOneDepartAt() {
      // Given a default postProcessor with 'numOfItineraries=1'
      ItineraryListFilterChain chain = createBuilder(false, false, 1).build();
      assertEquals(List.of(i1), chain.filter(List.of(i1, i2, i3)));
    }

    @Test
    void testPostProcessorWithMaxItinerariesFilterSetToOneArriveBy() {
      // Given a postProcessor with 'numOfItineraries=1' and 'arriveBy=true'
      ItineraryListFilterChain chain = createBuilder(true, false, 1).build();
      assertEquals(List.of(i3), chain.filter(List.of(i1, i2, i3)));
    }
  }

  @Nested
  class FlexSearchWindow {

    private final Itinerary flex = newItinerary(A, T11_00)
      .flex(T11_00, T11_30, B)
      .withIsSearchWindowAware(false)
      .build();
    private final Instant earliestDeparture = flex.startTime().plusMinutes(10).toInstant();
    private final Duration searchWindow = Duration.ofHours(7);

    /**
     * When the filtering of direct flex by the transit search window is deactivated, the direct
     * flex result should _not_ be filtered even though it starts before the earliest departure time.
     */
    @Test
    void keepDirectFlexWhenFilteringByEarliestDepartureIsDisabled() {
      ItineraryListFilterChain chain = createBuilder(true, false, 10)
        .withFilterDirectFlexBySearchWindow(false)
        .withSearchWindow(earliestDeparture, searchWindow)
        .build();
      assertEquals(toStr(List.of(flex)), toStr(chain.filter(List.of(flex))));
    }

    @Test
    void removeDirectFlexWhenFilteringByEarliestDepartureIsEnabled() {
      ItineraryListFilterChain chain = createBuilder(true, false, 10)
        .withFilterDirectFlexBySearchWindow(true)
        .withSearchWindow(earliestDeparture, searchWindow)
        .build();
      assertEquals(toStr(List.of()), toStr(chain.filter(List.of(flex))));
    }
  }

  @Nested
  class RemoveTransitWithHigherCostThanBestOnStreetOnlyTest {

    Itinerary walk;
    Itinerary bus;
    ItineraryListFilterChainBuilder builder = createBuilder(true, false, 20);

    @BeforeEach
    void setUpItineraries() {
      // given
      // Walk for 12 minute
      walk = newItinerary(A, T11_06).walk(D12m, E).build();
      // Not optimal, takes longer than walking
      bus = newItinerary(A).bus(21, T11_06, T11_28, E).build();
    }

    @Test
    void removeTransitWithHigherCostThanBestOnStreetOnlyDisabled() {
      // Allow non-optimal bus itinerary pass through
      ItineraryListFilterChain chain = builder
        .withRemoveTransitWithHigherCostThanBestOnStreetOnly(null)
        .withRemoveTransitIfWalkingIsBetter(false)
        .build();
      assertEquals(toStr(List.of(walk, bus)), toStr(chain.filter(List.of(walk, bus))));
    }

    @Test
    void removeTransitWithHigherCostThanBestOnStreetOnlyEnabled() {
      // Enable filter and remove bus itinerary
      ItineraryListFilterChain chain = builder
        .withRemoveTransitWithHigherCostThanBestOnStreetOnly(
          CostLinearFunction.of(Duration.ofSeconds(0), 1.0)
        )
        .build();
      assertEquals(toStr(List.of(walk)), toStr(chain.filter(List.of(walk, bus))));
    }
  }

  private static String toStringOfTags(List<SystemNotice> systemNotices) {
    return systemNotices == null
      ? "[]"
      : systemNotices.stream().map(SystemNotice::tag).toList().toString();
  }
}
