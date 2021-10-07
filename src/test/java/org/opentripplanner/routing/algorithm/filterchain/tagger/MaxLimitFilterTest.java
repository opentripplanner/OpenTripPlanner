package org.opentripplanner.routing.algorithm.filterchain.tagger;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

public class MaxLimitFilterTest implements PlanTestConstants {

    @Test
    public void name() {
        MaxLimitFilter subject = new MaxLimitFilter("Test", 3);
        assertEquals("Test", subject.name());
    }

    @Test
    public void testNormalFilterMaxLimit3() {
        MaxLimitFilter subject = new MaxLimitFilter("Test", 3);
        List<Itinerary> itineraries = getItineraries();
        assertEquals(toStr(itineraries), toStr(TaggerTestHelper.process(itineraries, subject)));
    }

    @Test
    public void testNormalFilterMaxLimit1() {
        MaxLimitFilter subject = new MaxLimitFilter("Test", 1);
        List<Itinerary> itineraries = getItineraries();
        assertEquals(toStr(List.of(itineraries.get(0))), toStr(TaggerTestHelper.process(itineraries, subject)));
    }

    @Test
    public void testNormalFilterMaxLimit0() {
        MaxLimitFilter subject = new MaxLimitFilter("Test", 0);
        List<Itinerary> itineraries = getItineraries();
        assertEquals(toStr(List.of()), toStr(TaggerTestHelper.process(itineraries, subject)));
    }

    private List<Itinerary> getItineraries() {
        Itinerary i1 = newItinerary(A, 6).walk(1, B).build();
        Itinerary i2 = newItinerary(A).bicycle(6, 8, B).build();
        Itinerary i3 = newItinerary(A).bus(21,6, 8, B).build();

        return List.of(i1, i2, i3);
    }
}