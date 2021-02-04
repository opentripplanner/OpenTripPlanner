package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

public class MaxLimitFilterTest implements PlanTestConstants {
  private final Itinerary i1 = newItinerary(A, 6).walk(1, B).build();
  private final Itinerary i2 = newItinerary(A).bicycle(6, 8, B).build();
  private final Itinerary i3 = newItinerary(A).bus(21,6, 8, B).build();

    private final List<Itinerary> itineraries = List.of(i1, i2, i3);

    @Test
    public void name() {
        MaxLimitFilter subject = new MaxLimitFilter("Test", 3);
        assertEquals("Test", subject.name());
    }

    @Test
    public void testNormalFilter() {
        MaxLimitFilter subject;

        subject = new MaxLimitFilter("Test", 3);
        assertEquals(toStr(itineraries), toStr(subject.filter(itineraries)));

        subject = new MaxLimitFilter("Test", 1);
        assertEquals(toStr(List.of(i1)), toStr(subject.filter(itineraries)));

        subject = new MaxLimitFilter("Test", 0);
        assertEquals(toStr(List.of()), toStr(subject.filter(itineraries)));
    }
}