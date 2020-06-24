package org.opentripplanner.routing.algorithm.filterchain;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.TestItineraryBuilder;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.A;
import static org.opentripplanner.model.plan.TestItineraryBuilder.E;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newTime;

public class ItineraryFilterChainBuilderTest {
  private static final int I3_LATE_START_TIME = 50;

  // And some itineraries, with some none optimal options
  // Short walk - 2 minutes - to destination:
  private final Itinerary i1 = newItinerary(A, 6).walk(2, E).build();

  // Not optimal, takes longer than walking
  private final Itinerary i2 = newItinerary(A).bus(21, 6, 9, E).build();

  // Not optimal, departure is very late
  private final Itinerary i3 = newItinerary(A).bus(20, I3_LATE_START_TIME, I3_LATE_START_TIME+1, E).build();


  @Test
  public void testDefaultFilterChain() {
    // Given a default chain
    ItineraryFilter chain = new ItineraryFilterChainBuilder(config(false, false, 10)).build();

    assertEquals(toStr(List.of(i1, i3)), toStr(chain.filter(List.of(i1, i2, i3))));
  }

  @Test
  public void testFilterChainWithLateDepartureFilterSet() {
    // Given a "default" chain
    ItineraryFilterChainBuilder builder = new ItineraryFilterChainBuilder(config(false, false, 10));
    // with latest-departure-time-limit set
    builder.withLatestDepartureTimeLimit(TestItineraryBuilder.newTime(40).toInstant());
    ItineraryFilter chain = builder.build();

    assertEquals(toStr(List.of(i1)), toStr(chain.filter(List.of(i1, i3))));
  }

  @Test
  public void testFilterChainWithMaxItinerariesFilterSet() {
    // Given:
    ItineraryFilter chain;
    Itinerary i1 = newItinerary(A).bus(21, 5, 10, E).build();
    Itinerary i2 = newItinerary(A).bus(31, 7, 12, E).build();
    Itinerary i3 = newItinerary(A).bus(41, 9, 14, E).build();

    // Given a default chain with 'numOfItineraries=2'
    chain = new ItineraryFilterChainBuilder(config(false, false, 2)).build();
    assertEquals(List.of(i1, i2), chain.filter(List.of(i1, i2, i3)));

    // Given a default chain with 'numOfItineraries=1'
    chain = new ItineraryFilterChainBuilder(config(false, false, 1)).build();
    assertEquals(List.of(i1), chain.filter(List.of(i1, i2, i3)));

    // Given a chain with 'numOfItineraries=1' and 'arriveBy=true'
    chain = new ItineraryFilterChainBuilder(config(true, false, 1)).build();
    assertEquals(List.of(i3), chain.filter(List.of(i1, i2, i3)));
  }

  @Test
  public void testDebugFilterChain() {
    // Given a filter-chain with debugging enabled
    ItineraryFilter chain = new ItineraryFilterChainBuilder(config(false, true, 2))
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


  /* private methods */

  private FilterChainParameters config(boolean arriveBy, boolean debug, int numOfItineraries) {
    return new FilterChainParameters() {
      @Override
      public boolean arriveBy() { return arriveBy; }

      @Override
      public int maxNumberOfItineraries() {
        return numOfItineraries;
      }

      @Override
      public List<GroupBySimilarity> groupBySimilarity() {
        return List.of(
            // Keep 1 itinerary if a group of itineraries is 85% similar
            new GroupBySimilarity(0.85, 1),
            // Keep 'minLimit' itineraries if a group of itineraries is 68% similar
            new GroupBySimilarity(0.68, numOfItineraries)
        );
      }

      @Override
      public boolean debug() { return debug; }
    };
  }
}