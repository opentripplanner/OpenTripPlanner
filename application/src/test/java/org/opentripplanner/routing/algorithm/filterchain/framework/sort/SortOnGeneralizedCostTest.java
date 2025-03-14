package org.opentripplanner.routing.algorithm.filterchain.framework.sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.routing.algorithm.filterchain.framework.sort.SortOrderComparator.generalizedCostComparator;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

public class SortOnGeneralizedCostTest implements PlanTestConstants {

  @Test
  public void sortOnCost() {
    List<Itinerary> result;

    // Given: a walk(50m), bus(30m) and rail(20m) alternatives without generalizedCost or transfers
    // We prioritize walking when setting the generalizedCost
    Itinerary walk = newItinerary(A, 0).walk(50, E).build(600);
    Itinerary bus = newItinerary(A).bus(21, 0, 30, E).build(602);
    Itinerary rail = newItinerary(A).rail(110, 0, 20, E).build(601);

    // Add some cost - we prioritize walking

    // When: sorting
    result = Stream.of(walk, bus, rail)
      .sorted(generalizedCostComparator())
      .collect(Collectors.toList());

    // Then: expect rail(1/3 of walk time), bus(2/3 of walk time) and walk
    assertEquals(toStr(List.of(walk, rail, bus)), toStr(result));
  }

  @Test
  public void sortOnCostAndNumOfTransfers() {
    List<Itinerary> result;
    int COST = 300;

    // Given: 3 itineraries with 0, 1, and 2 number-of-transfers and the same cost
    Itinerary walk = newItinerary(A, 0).walk(50, E).build(COST);
    Itinerary bus1 = newItinerary(A).bus(21, 0, 10, B).bus(31, 30, 45, E).build(COST);
    Itinerary bus2 = newItinerary(A)
      .bus(21, 0, 10, B)
      .bus(31, 30, 45, C)
      .bus(41, 30, 45, E)
      .build(COST);

    // When: sorting
    result = Stream.of(bus2, walk, bus1)
      .sorted(generalizedCostComparator())
      .collect(Collectors.toList());

    // Then: expect bus to be better than walking
    assertEquals(toStr(List.of(walk, bus1, bus2)), toStr(result));
  }
}
