package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

public class TransitGeneralizedCostFilterTest implements PlanTestConstants {

  @Test
  public void filterWithoutWaitCost() {
    // Create a filter with f(x) = 600 + 2x, without any penalty for waiting at the beginning or end.
    // Remove itineraries with a cost equivalent of 10 minutes and twice the min itinerary cost.
    final TransitGeneralizedCostFilter subject = new TransitGeneralizedCostFilter(
      CostLinearFunction.of(Duration.ofMinutes(10), 2.0),
      0.0
    );

    // Walk all the way, not touched by the filter even if cost(7200) is higher than transit limit.
    Itinerary i1 = newItinerary(A, T11_06).walk(60, E).build();

    // Optimal bus ride. Cost: 120 + 3 * 60 = 300  => Limit: 1200
    Itinerary i2 = newItinerary(A).bus(21, T11_06, T11_09, E).build();

    // Within cost limit. Cost: 120 + 18 * 60 = 1200
    Itinerary i3 = newItinerary(A).bus(31, T11_07, T11_25, E).build();

    // Outside cost limit. Cost: 120 + 19 * 60 = 1260
    Itinerary i4 = newItinerary(A).bus(41, T11_08, T11_27, E).build();

    var all = List.of(i1, i2, i3, i4);

    // Expect - i4 to be dropped
    assertEquals(toStr(List.of(i1, i2, i3)), toStr(subject.removeMatchesForTest(all)));
  }

  @Test
  public void filterWithWaitCostSameDepartureTime() {
    // Create a filter with f(x) = 0 + 2x and a penalty of 0.5 at the beginning and end.
    // Remove itineraries with a cost equivalent of twice the itinerary cost plus half of the
    // waiting time.
    final TransitGeneralizedCostFilter subject = new TransitGeneralizedCostFilter(
      CostLinearFunction.of(Cost.ZERO, 2.0),
      0.5
    );

    // Walk all the way, not touched by the filter even if cost(7200) is higher than transit limit.
    Itinerary i1 = newItinerary(A, T11_06).walk(60, E).build();

    // Optimal bus ride. Cost: 120 + 5 * 60 = 420  => Limit: 840 + half of waiting time
    Itinerary i2 = newItinerary(A).bus(21, T11_00, T11_05, E).build();

    // Within cost limit. Cost: 120 + 10 * 60 = 720, limit 840 + 0 -> Ok
    Itinerary i3 = newItinerary(A).bus(31, T11_00, T11_10, E).build();

    // Outside cost limit. Cost: 120 + 15 * 60 = 1020, limit 840 + 0 -> Filtered
    Itinerary i4 = newItinerary(A).bus(41, T11_00, T11_15, E).build();

    var all = List.of(i1, i2, i3, i4);

    // Expect - i4 to be dropped
    assertEquals(toStr(List.of(i1, i2, i3)), toStr(subject.removeMatchesForTest(all)));
  }

  @Test
  public void filterWithWaitCostDifferentDepartureTime() {
    // Create a filter with f(x) = 0 + 2x and a penalty of 0.5 at the beginning and end.
    // Remove itineraries with a cost equivalent of twice the itinerary cost plus half of the
    // waiting time.
    final TransitGeneralizedCostFilter subject = new TransitGeneralizedCostFilter(
      CostLinearFunction.of(Cost.ZERO, 2.0),
      0.5
    );

    // Walk all the way, not touched by the filter even if cost(7200) is higher than transit limit.
    Itinerary i1 = newItinerary(A, T11_06).walk(60, E).build();

    // Optimal bus ride. Cost: 120 + 5 * 60 = 420  => Limit: 840 + half of waiting time
    Itinerary i2 = newItinerary(A).bus(21, T11_00, T11_05, E).build();

    // Outside cost limit. Cost: 120 + 15 * 60 = 1020, limit 840 + 0.5 * 5 * 60 = 990 -> Filtered
    Itinerary i3 = newItinerary(A).bus(31, T11_05, T11_20, E).build();

    // Within cost limit. Cost: 120 + 15 * 60 = 1020, limit 840 + 0.5 * 5 * 60 = 1140 -> Ok
    Itinerary i4 = newItinerary(A).bus(41, T11_10, T11_25, E).build();

    var all = List.of(i1, i2, i3, i4);

    // Expect - i3 to be dropped
    assertEquals(toStr(List.of(i1, i2, i4)), toStr(subject.removeMatchesForTest(all)));
  }
}
