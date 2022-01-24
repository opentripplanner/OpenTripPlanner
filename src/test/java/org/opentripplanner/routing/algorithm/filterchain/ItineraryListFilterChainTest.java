package org.opentripplanner.routing.algorithm.filterchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_ARRIVAL_TIME;
import static org.opentripplanner.model.plan.SortOrder.STREET_AND_DEPARTURE_TIME;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newTime;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;


/**
 * This class test the whole filter chain with a few test cases. Each filter should be tested
 * with a unit test. This is just a some test on top of the other filter unit-tests.
 */
public class ItineraryListFilterChainTest implements PlanTestConstants {
  private static final int I3_LATE_START_TIME = T11_33;

  private Itinerary i1;
  private Itinerary i2;
  private Itinerary i3;

  @BeforeEach
  public void setUpItineraries() {
    // Add some itineraries, with some none optimal options
    // Short walk - 2 minutes - to destination:
    i1 = newItinerary(A, T11_06).walk(D2m, E).build();

    // Not optimal, takes longer than walking
    i2 = newItinerary(A).bus(21, T11_06, T11_09, E).build();

    // Not optimal, departure is very late
    i3 = newItinerary(A)
            .bus(20, I3_LATE_START_TIME, I3_LATE_START_TIME + D1m, E)
            .build();
  }


  @Test
  public void testDefaultFilterChain() {
    // Given a default chain
    ItineraryListFilterChain chain = createBuilder(false, false, 10).build();

    assertEquals(toStr(List.of(i1, i3)), toStr(chain.filter(List.of(i1, i2, i3))));
  }

  @Test
  public void testFilterChainWithLateDepartureFilterSet() {
    // Given a "default" chain
    ItineraryListFilterChain chain =
        createBuilder(false, false, 10)
            // with latest-departure-time-limit set
            .withLatestDepartureTimeLimit(TestItineraryBuilder.newTime(T11_32).toInstant())
            .build();

    assertEquals(toStr(List.of(i1)), toStr(chain.filter(List.of(i1, i2, i3))));
  }

  @Nested
  class MaxItinerariesBuilderTest {

    @BeforeEach
    public void setUpItineraries() {
      i1 = newItinerary(A).bus(21, T11_05, T11_10, E).build();
      i2 = newItinerary(A).bus(31, T11_07, T11_12, E).build();
      i3 = newItinerary(A).bus(41, T11_09, T11_14, E).build();
    }

    @Test
    public void testPostProcessorWithMaxItinerariesFilterSetToTwo() {
      // Given a default postProcessor with 'numOfItineraries=2'
      ItineraryListFilterChain chain = createBuilder(false, false, 2).build();
      assertEquals(List.of(i1, i2), chain.filter(List.of(i1, i2, i3)));
    }

    @Test
    public void testPostProcessorWithMaxItinerariesFilterSetToOneDepartAt() {
      // Given a default postProcessor with 'numOfItineraries=1'
      ItineraryListFilterChain chain = createBuilder(false, false, 1).build();
      assertEquals(List.of(i1), chain.filter(List.of(i1, i2, i3)));

    }

    @Test
    public void testPostProcessorWithMaxItinerariesFilterSetToOneArriveBy() {
      // Given a postProcessor with 'numOfItineraries=1' and 'arriveBy=true'
      ItineraryListFilterChain chain = createBuilder(true, false, 1).build();
      assertEquals(List.of(i3), chain.filter(List.of(i1, i2, i3)));
    }
  }

  @Test
  public void testDebugFilterChain() {
    // Given a filter-chain with debugging enabled
    ItineraryListFilterChain chain = createBuilder(false, true, 3)
        .withLatestDepartureTimeLimit(newTime(I3_LATE_START_TIME - 1).toInstant())
        .build();

    // Walk first, then transit sorted on arrival-time
    assertEquals(toStr(List.of(i1, i2, i3)), toStr(chain.filter(List.of(i1, i2, i3))));
    assertTrue(i1.systemNotices.isEmpty());
    assertFalse(i2.systemNotices.isEmpty());
    assertFalse(i3.systemNotices.isEmpty());
    assertEquals("transit-vs-street-filter", i2.systemNotices.get(0).tag);
    assertEquals("latest-departure-time-limit", i3.systemNotices.get(0).tag);
  }

  @Nested
  class RemoveTransitWithHigherCostThanBestOnStreetOnlyTest {

    Itinerary walk;
    Itinerary bus;
    ItineraryListFilterChainBuilder builder = createBuilder(true, false, 20);

    @BeforeEach
    public void setUpItineraries() {
      // given
      // Walk for 12 minute
      walk = newItinerary(A, T11_06).walk(D12m, E).build();
      // Not optimal, takes longer than walking
      bus = newItinerary(A).bus(21, T11_06, T11_28, E).build();
    }

    @Test
    public void removeTransitWithHigherCostThanBestOnStreetOnlyDisabled() {
      // Disable filter and allow none optimal bus itinerary pass through
      ItineraryListFilterChain chain = builder.withRemoveTransitWithHigherCostThanBestOnStreetOnly(false).build();
      assertEquals(toStr(List.of(walk, bus)), toStr(chain.filter(List.of(walk, bus))));
    }

    @Test
    public void removeTransitWithHigherCostThanBestOnStreetOnlyEnabled() {
      // Enable filter and remove bus itinerary
      ItineraryListFilterChain chain = builder.withRemoveTransitWithHigherCostThanBestOnStreetOnly(true).build();
      assertEquals(toStr(List.of(walk)), toStr(chain.filter(List.of(walk, bus))));
    }
  }

  @Test
  public void removeAllWalkingOnly() {
    ItineraryListFilterChain chain = createBuilder(false, false, 20)
        .withRemoveWalkAllTheWayResults(true)
        .build();

    Itinerary walk = newItinerary(A, T11_06).walk(D10m, E).build();
    Itinerary bus = newItinerary(A).bus(21, T11_06, T11_12, E).build();

    assertEquals(toStr(List.of(bus)), toStr(chain.filter(List.of(walk, bus))));
  }

  @org.junit.Test
  public void groupByTheLongestItineraryAndTwoGroups() {
    ItineraryListFilterChain chain = createBuilder(false, false, 20)
            .addGroupBySimilarity(GroupBySimilarity.createWithOneItineraryPerGroup(.5))
            .build();

    // Group 1
    Itinerary i1 = newItinerary(A, 6)
            .walk(240, C)
            .build();

    // Group 2, with 2 itineraries
    Itinerary i2 = newItinerary(A)
            .bus(1, 0, 50, B)
            .bus(11, 52, 100, C)
            .build();
    Itinerary i3 = newItinerary(A)
            .bus(1, 0, 50, B)
            .bus(12, 55, 102, C)
            .build();

    List<Itinerary> input = List.of(i1, i2, i3);

    // With min Limit = 1, expect the best trips from both groups
    chain.filter(input);

    assertFalse(i1.isFlaggedForDeletion());
    assertFalse(i2.isFlaggedForDeletion());
    assertTrue(i3.isFlaggedForDeletion());
  }

  @Test void testRoutingErrorsOriginDestinationTooCloseTest() {
    ItineraryListFilterChain chain = createBuilder(false, false, 20)
            .withRemoveWalkAllTheWayResults(true)
            .withRemoveTransitWithHigherCostThanBestOnStreetOnly(true)
            .build();

    Itinerary walk = newItinerary(A, T11_06).walk(D10m, E).build();
    Itinerary bus = newItinerary(A).bus(21, T11_06, T11_28, E).build();

    assertTrue(chain.filter(List.of(walk, bus)).isEmpty());

    final List<RoutingError> routingErrors = chain.getRoutingErrors();
    assertEquals(1, routingErrors.size());
    assertEquals(RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT, routingErrors.get(0).code);
  }

  @Test void routingErrorsOutsideWindowTest() {
    var chain = createBuilder(false, false, 20)
            .withRemoveWalkAllTheWayResults(true)
            .withLatestDepartureTimeLimit(Instant.from(newTime(T11_00)))
            .build();

    Itinerary bus = newItinerary(A).bus(21, T11_06, T11_23, E).build();

    assertTrue(chain.filter(List.of(bus)).isEmpty());

    final List<RoutingError> routingErrors = chain.getRoutingErrors();
    assertEquals(1, routingErrors.size());
    assertEquals(RoutingErrorCode.NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW, routingErrors.get(0).code);
  }

  /* private methods */

  private ItineraryListFilterChainBuilder createBuilder(boolean arriveBy, boolean debug, int numOfItineraries) {
    var sortOrder = arriveBy ? STREET_AND_DEPARTURE_TIME : STREET_AND_ARRIVAL_TIME;
    return new ItineraryListFilterChainBuilder(sortOrder)
        .withMaxNumberOfItineraries(numOfItineraries)
        .withRemoveTransitWithHigherCostThanBestOnStreetOnly(true)
        .withDebugEnabled(debug);
  }
}