package org.opentripplanner.routing.algorithm.filterchain;

import lombok.val;
import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newTime;


/**
 * This class test the hole filter chain with a few test cases. Each filter should be tested
 * with a unit test. This is just a some test on top of the other filter unit-tests.
 */
public class ItineraryFilterChainTest implements PlanTestConstants {
  private static final int I3_LATE_START_TIME = T11_33;

  // And some itineraries, with some none optimal options
  // Short walk - 2 minutes - to destination:
  private final Itinerary i1 = newItinerary(A, T11_06).walk(D2m, E).build();

  // Not optimal, takes longer than walking
  private final Itinerary i2 = newItinerary(A).bus(21, T11_06, T11_09, E).build();

  // Not optimal, departure is very late
  private final Itinerary i3 = newItinerary(A)
      .bus(20, I3_LATE_START_TIME, I3_LATE_START_TIME + D1m, E)
      .build();


  @Test
  public void testDefaultFilterChain() {
    // Given a default chain
    ItineraryFilter chain = createBuilder(false, false, 10).build();

    assertEquals(toStr(List.of(i1, i3)), toStr(chain.filter(List.of(i1, i2, i3))));
  }

  @Test
  public void testFilterChainWithLateDepartureFilterSet() {
    // Given a "default" chain
    ItineraryFilter chain =
        createBuilder(false, false, 10)
            // with latest-departure-time-limit set
            .withLatestDepartureTimeLimit(TestItineraryBuilder.newTime(T11_32).toInstant())
            .build();

    assertEquals(toStr(List.of(i1)), toStr(chain.filter(List.of(i1, i2, i3))));
  }

  @Test
  public void testFilterChainWithMaxItinerariesFilterSet() {
    // Given:
    ItineraryFilter chain;
    Itinerary i1 = newItinerary(A).bus(21, T11_05, T11_10, E).build();
    Itinerary i2 = newItinerary(A).bus(31, T11_07, T11_12, E).build();
    Itinerary i3 = newItinerary(A).bus(41, T11_09, T11_14, E).build();

    // Given a default chain with 'numOfItineraries=2'
    chain = createBuilder(false, false, 2).build();
    assertEquals(List.of(i1, i2), chain.filter(List.of(i1, i2, i3)));

    // Given a default chain with 'numOfItineraries=1'
    chain = createBuilder(false, false, 1).build();
    assertEquals(List.of(i1), chain.filter(List.of(i1, i2, i3)));

    // Given a chain with 'numOfItineraries=1' and 'arriveBy=true'
    chain = createBuilder(true, false, 1).build();
    assertEquals(List.of(i3), chain.filter(List.of(i1, i2, i3)));
  }

  @Test
  public void testDebugFilterChain() {
    // Given a filter-chain with debugging enabled
    ItineraryFilter chain = createBuilder(false, true, 3)
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

  @Test
  public void removeTransitWithHigherCostThanBestOnStreetOnly() {
    val builder= createBuilder(true, false, 20);
    ItineraryFilter chain;

    // given
    // Walk for 12 minute
    Itinerary walk = newItinerary(A, T11_06).walk(D12m, E).build();
    // Not optimal, takes longer than walking
    Itinerary bus = newItinerary(A).bus(21, T11_06, T11_28, E).build();

    // Disable filter and allow none optimal bus itinerary pass through
    chain = builder.withRemoveTransitWithHigherCostThanBestOnStreetOnly(false).build();
    assertEquals(toStr(List.of(walk, bus)), toStr(chain.filter(List.of(walk, bus))));

    // Enable filter and remove bus itinerary
    chain = builder.withRemoveTransitWithHigherCostThanBestOnStreetOnly(true).build();
    assertEquals(toStr(List.of(walk)), toStr(chain.filter(List.of(walk, bus))));
  }

  @Test
  public void removeAllWalkingOnly() {
    var chain = createBuilder(false, false, 20)
        .withRemoveWalkAllTheWayResults(true)
        .build();

    Itinerary walk = newItinerary(A, T11_06).walk(D10m, E).build();
    Itinerary bus = newItinerary(A).bus(21, T11_06, T11_12, E).build();

    assertEquals(toStr(List.of(bus)), toStr(chain.filter(List.of(walk, bus))));
  }



  /* private methods */

  private ItineraryFilterChainBuilder createBuilder(boolean arriveBy, boolean debug, int numOfItineraries) {
    return new ItineraryFilterChainBuilder(arriveBy)
        .withMaxNumberOfItineraries(numOfItineraries)
        .withRemoveTransitWithHigherCostThanBestOnStreetOnly(true)
        .withDebugEnabled(debug);
  }
}