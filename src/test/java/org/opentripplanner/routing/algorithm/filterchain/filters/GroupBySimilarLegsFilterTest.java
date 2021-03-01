package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

/**
 * Smoke test on the GroupBySimilarLegsFilter filter, all parts of the filter have their own
 * unit tests.
 */
public class GroupBySimilarLegsFilterTest implements PlanTestConstants {

  public static final SortOnGeneralizedCost SORT_ON_COST = new SortOnGeneralizedCost();

  @Test
  public void groupByTheLongestItineraryAndTwoGroups() {
    List<Itinerary> result;

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
    result = new GroupBySimilarLegsFilter(.5, 1, SORT_ON_COST).filter(input);

    assertEquals(toStr(List.of(i1, i2)), toStr(result));
  }
}