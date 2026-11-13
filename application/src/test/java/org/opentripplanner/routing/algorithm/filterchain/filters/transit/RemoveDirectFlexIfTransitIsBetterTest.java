package org.opentripplanner.routing.algorithm.filterchain.filters.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newTime;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.basic.CostLinearFunction;
import org.opentripplanner.ext.carpooling.model.CarpoolLeg;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

class RemoveDirectFlexIfTransitIsBetterTest implements PlanTestConstants {

  @Test
  void removeDirectFlexWithHigherCost() {
    // Create a filter with f(x) = 0 + 2x, without any penalty for waiting at the beginning or end.
    var subject = new RemoveDirectFlexIfTransitIsBetter(CostLinearFunction.of(Cost.ZERO, 2.0), 0.0);

    // Walk all the way, not touched by the filter
    Itinerary i1 = newItinerary(A, T11_06).walk(60, E).build();

    // Bus ride. Cost: 120 + 10 * 60 = 720 => Limit: 1440
    Itinerary i2 = newItinerary(A).bus(21, T11_00, T11_10, E).build();

    // Direct flex within cost limit. Cost: 1440 -> Ok
    Itinerary i3 = newItinerary(A, T11_00).flex(T11_00, T11_10, E).build(1440);

    // Direct flex outside cost limit. Cost: 1441 -> Filtered
    Itinerary i4 = newItinerary(A, T11_00).flex(T11_00, T11_10, E).build(1441);

    var all = List.of(i1, i2, i3, i4);

    // Expect - i4 to be dropped
    assertEquals(toStr(List.of(i1, i2, i3)), toStr(subject.removeMatchesForTest(all)));
  }

  @Test
  void removeDirectFlexWithWaitCost() {
    // Create a filter with f(x) = 0 + 2x and a penalty of 0.5 at the beginning and end.
    var subject = new RemoveDirectFlexIfTransitIsBetter(CostLinearFunction.of(Cost.ZERO, 2.0), 0.5);

    // Bus ride. Cost: 120 + 10 * 60 = 720 => Limit: 1440 + half of waiting time
    Itinerary i1 = newItinerary(A).bus(21, T11_00, T11_10, E).build();

    // Direct flex 10 minutes later. Limit: 1440 + 0.5 * 10 * 60 = 1740. Cost: 1740 -> Ok
    Itinerary i2 = newItinerary(A, T11_10).flex(T11_10, T11_20, E).build(1740);

    // Direct flex 10 minutes later. Limit: 1740. Cost: 1741 -> Filtered
    Itinerary i3 = newItinerary(A, T11_10).flex(T11_10, T11_20, E).build(1741);

    var all = List.of(i1, i2, i3);

    // Expect - i3 to be dropped
    assertEquals(toStr(List.of(i1, i2)), toStr(subject.removeMatchesForTest(all)));
  }

  @Test
  void directFlexIsUsedAsReference() {
    // Create a filter with f(x) = 0 + 2x, without any penalty for waiting at the beginning or end.
    var subject = new RemoveDirectFlexIfTransitIsBetter(CostLinearFunction.of(Cost.ZERO, 2.0), 0.0);

    // Direct flex. Cost: 100 => Limit: 200
    Itinerary i1 = newItinerary(A, T11_00).flex(T11_00, T11_10, E).build(100);

    // Direct flex. Cost: 1000 -> Filtered by i1, but within the limit of the bus
    Itinerary i2 = newItinerary(A, T11_00).flex(T11_00, T11_10, E).build(1000);

    // Bus ride, not touched by the filter. Cost: 120 + 10 * 60 = 720 => Limit: 1440
    Itinerary i3 = newItinerary(A).bus(21, T11_00, T11_10, E).build();

    var all = List.of(i1, i2, i3);

    // Expect - i2 to be dropped
    assertEquals(toStr(List.of(i1, i3)), toStr(subject.removeMatchesForTest(all)));
  }

  @Test
  void carpoolIsNotFiltered() {
    // Create a filter with f(x) = 0 + 2x, without any penalty for waiting at the beginning or end.
    var subject = new RemoveDirectFlexIfTransitIsBetter(CostLinearFunction.of(Cost.ZERO, 2.0), 0.0);

    // Bus ride. Cost: 120 + 10 * 60 = 720 => Limit: 1440
    Itinerary i1 = newItinerary(A).bus(21, T11_00, T11_10, E).build();

    // Carpool, counts as a flexible trip but is not transit. Cost: 2000
    Itinerary i2 = Itinerary.ofDirect(
      List.of(
        CarpoolLeg.of()
          .withFrom(A)
          .withTo(E)
          .withStartTime(newTime(T11_00))
          .withEndTime(newTime(T11_10))
          .withGeneralizedCost(2000)
          .build()
      )
    )
      .withGeneralizedCost(Cost.costOfSeconds(2000))
      .build();

    var all = List.of(i1, i2);

    // Expect - nothing to be dropped
    assertEquals(toStr(all), toStr(subject.removeMatchesForTest(all)));
  }
}
