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
    Itinerary walk = newItinerary(A, 0).walk(50, E).build();
    Itinerary bus = newItinerary(A).bus(21, 0, 30, E).build();
    Itinerary rail = newItinerary(A).rail(110, 0, 20, E).build();

    // Add some cost - we prioritize walking
    walk.setGeneralizedCost(600);
    bus.setGeneralizedCost(600 + 2);
    rail.setGeneralizedCost(600 + 1);

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

    // Given: 3 itineraries with 0, 1, and 2 number-of-transfers and the same cost
    Itinerary walk = newItinerary(A, 0).walk(50, E).build();
    Itinerary bus1 = newItinerary(A).bus(21, 0, 10, B).bus(31, 30, 45, E).build();
    Itinerary bus2 = newItinerary(A)
      .bus(21, 0, 10, B)
      .bus(31, 30, 45, C)
      .bus(41, 30, 45, E)
      .build();

    // Add some cost - we prioritize bus with the lowest cost
    walk.setGeneralizedCost(300);
    bus1.setGeneralizedCost(300);
    bus2.setGeneralizedCost(300);

    // When: sorting
    result = Stream.of(bus2, walk, bus1)
      .sorted(generalizedCostComparator())
      .collect(Collectors.toList());

    // Then: expect bus to be better than walking
    assertEquals(toStr(List.of(walk, bus1, bus2)), toStr(result));
  }
}
