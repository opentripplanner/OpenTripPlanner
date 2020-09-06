package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.api.request.RequestFunctions;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.A;
import static org.opentripplanner.model.plan.TestItineraryBuilder.E;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

public class TransitGeneralizedCostFilterTest {

  // Create a filter with f(x) = 600 + 2x
  // Remove itineraries with a cost equivalent of 10 minutes and twice the min itinerary cost.
  private final ItineraryFilter subject = new TransitGeneralizedCostFilter(
      RequestFunctions.createLinearFunction(600, 2.0)
  );

  @Test
  public void name() {
    assertEquals("transit-cost-filter", subject.name());
  }

  @Test
  public void filter() {


    // Walk all the way, not touched by the filter even if cost(7200) is higher than transit limit.
    Itinerary i1 = newItinerary(A, 6).walk(60, E).build();

    // Optimal bus ride. Cost: 120 + 3 * 60 = 300  => Limit: 1200
    Itinerary i2 = newItinerary(A).bus(21, 6, 9, E).build();

    // Within cost limit. Cost: 120 + 18 * 60 = 1200
    Itinerary i3 = newItinerary(A).bus(31, 7, 25, E).build();

    // Outside cost limit. Cost: 120 + 19 * 60 = 1260
    Itinerary i4 = newItinerary(A).bus(41, 8, 27, E).build();

    var all = List.of(i1, i2, i3, i4);

    // Expect - i4 to be dropped
    assertEquals(toStr(List.of(i1, i2, i3)), toStr(subject.filter(all)));
  }
}