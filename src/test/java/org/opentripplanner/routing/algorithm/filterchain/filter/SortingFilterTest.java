package org.opentripplanner.routing.algorithm.filterchain.filter;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.routing.algorithm.filterchain.comparator.SortOrderComparator.generalizedCostComparator;

import java.util.List;
import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

public class SortingFilterTest implements PlanTestConstants {

    @Test
    public void sortWorksWithOneOrEmptyList() {
        SortingFilter filter = new SortingFilter(generalizedCostComparator());

        // Expect sort to no fail on an empty list
        assertEquals(List.of(), filter.filter(List.of()));


        // Given a list with one itinerary
        List<Itinerary> list = List.of(newItinerary(A).bus(31, 0, 30, E).build());

        // Then: expect nothing to happen to it
        assertEquals(toStr(list), toStr(filter.filter(list)));
    }
}