package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

public class ReduceTimeTableVariationFilterTest {

  @Test
  public void aLongWalkWithHighCostShouldBeRemoved() {
    // Lowest cost - preferred itinerary
    final Itinerary i1 = newItinerary(TestItineraryBuilder.A, 0)
        .walk(5, TestItineraryBuilder.B)
        .bus(1, 7, 50, TestItineraryBuilder.E)
        .walk(5, TestItineraryBuilder.F)
        .build();

    // Access: Start after i1, but walk longer => higher cost
    final Itinerary i2 = newItinerary(TestItineraryBuilder.A, 1)
        // Walk 1 minute more
        .walk(6, TestItineraryBuilder.C)
        // Ride 1 minutes less
        .bus(1, 8, 50, TestItineraryBuilder.E)
        .walk(5, TestItineraryBuilder.F)
        .build();

    // Egress: arrive-before, but walk is longer => higher cost
    final Itinerary i3 = newItinerary(TestItineraryBuilder.A, 0)
        .walk(5, TestItineraryBuilder.B)
        .bus(1, 7, 48, TestItineraryBuilder.D)
        // Walk 3 minute more and arrive 1 minute before
        .walk(6, TestItineraryBuilder.F)
        .build();

    ItineraryFilter filter = new ReduceTimeTableVariationFilter(0);

    assertEquals(toStr(List.of(i1)), toStr(filter.filter(List.of(i1, i2, i3))));
  }

  @Test
  public void shortAdditionalTransitLegsInTheBeginningOrEndShouldBeRemoved() {
    // Lowest cost - preferred itinerary
    final Itinerary i1 = newItinerary(TestItineraryBuilder.C)
        .rail(1, 10, 50, TestItineraryBuilder.D)
        .build();

    // Access: Start after i1, but extra bus in the beginning => higher cost
    final Itinerary i2 = newItinerary(TestItineraryBuilder.A)
        .bus(21, 11, 13, TestItineraryBuilder.B)
        .rail(1, 15, 50, TestItineraryBuilder.D)
        .build();

    // Egress: arrive-before, but extra bus in the end of the trip => higher cost
    final Itinerary i3 = newItinerary(TestItineraryBuilder.C)
        .rail(1, 10, 45, TestItineraryBuilder.E)
        .bus(31, 46, 49, TestItineraryBuilder.D)
        .build();

    ItineraryFilter filter = new ReduceTimeTableVariationFilter(181);

    assertEquals(toStr(List.of(i1)), toStr(filter.filter(List.of(i1, i2, i3))));
  }
}