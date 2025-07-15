package org.opentripplanner.routing.algorithm.filterchain.filters.transit.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

class RemoveOtherThanSameLegsMaxGeneralizedCostTest implements PlanTestConstants {

  @Test
  public void testFilter() {
    Itinerary first = newItinerary(A)
      .rail(20, T11_05, T11_14, B)
      .bus(30, T11_16, T11_20, C)
      .build();

    Itinerary second = newItinerary(A).rail(20, T11_05, T11_14, B).walk(D10m, C).build();

    var subject = new RemoveOtherThanSameLegsMaxGeneralizedCost(2.0);
    assertEquals(List.of(second), subject.flagForRemoval(List.of(first, second)));
  }

  @Test
  public void testFilterNothingInCommon() {
    Itinerary first = newItinerary(A)
      .rail(20, T11_05, T11_14, B)
      .bus(30, T11_16, T11_20, C)
      .build();

    Itinerary second = newItinerary(A).rail(40, T11_05, T11_14, B).walk(D10m, C).build();

    var subject = new RemoveOtherThanSameLegsMaxGeneralizedCost(2.0);
    assertEquals(List.of(), subject.flagForRemoval(List.of(first, second)));
  }

  @Test
  public void testFilterInaccurateLegCosts() {
    // Regression test that verifies that we don't crash in the case where the sum of the leg costs
    // is larger than the itinerary cost.
    int itineraryCost = 100;
    Itinerary first = newItinerary(A)
      .rail(20, T11_05, T11_14, B, 400)
      .walk(60 * 4, C)
      .build(itineraryCost);

    Itinerary second = newItinerary(A)
      .rail(20, T11_05, T11_14, B, 400)
      .walk(D10m, C)
      .build(itineraryCost);

    var subject = new RemoveOtherThanSameLegsMaxGeneralizedCost(2.0);
    assertEquals(List.of(), subject.flagForRemoval(List.of(first, second)));
  }
}
