package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.algorithm.filterchain.ListSection;

public class MaxLimitFilterTest implements PlanTestConstants {
    private final static Itinerary i1 = newItinerary(A, 6).walk(1, B).build();
    private final static Itinerary i2 = newItinerary(A).bicycle(6, 8, B).build();
    private final static Itinerary i3 = newItinerary(A).bus(21,6, 8, B).build();

    @Test
    public void name() {
        MaxLimitFilter subject = new MaxLimitFilter("Test", 3);
        assertEquals("Test", subject.name());
    }

    @Test
    public void testNormalFilterMaxLimit3() {
        MaxLimitFilter subject = new MaxLimitFilter("Test", 3);
        List<Itinerary> itineraries = List.of(i1, i2, i3);
        assertEquals(
                toStr(itineraries),
                toStr(DeletionFlaggerTestHelper.process(itineraries, subject))
        );
    }

    @Test
    public void testNormalFilterMaxLimit1() {
        MaxLimitFilter subject = new MaxLimitFilter("Test", 1);
        List<Itinerary> itineraries = List.of(i1, i2, i3);
        assertEquals(
                toStr(List.of(i1)),
                toStr(DeletionFlaggerTestHelper.process(itineraries, subject))
        );
    }

    @Test
    public void testNormalFilterMaxLimit0() {
        MaxLimitFilter subject = new MaxLimitFilter("Test", 0);
        List<Itinerary> itineraries = List.of(i1, i2, i3);
        var result = DeletionFlaggerTestHelper.process(itineraries, subject);
        assertEquals(toStr(List.of()), toStr(result));
    }

    @Test
    public void testCropHead() {
        MaxLimitFilter subject = new MaxLimitFilter("Test", 1, ListSection.HEAD, null);
        List<Itinerary> itineraries = List.of(i1, i2, i3);
        var result = DeletionFlaggerTestHelper.process(itineraries, subject);
        assertEquals(toStr(List.of(i3)), toStr(result));
    }

    @Test
    public void testCropTailAndSubscribe() {
        var subscribeResult = new ArrayList<Itinerary>();
        var subject = new MaxLimitFilter("Test", 2, ListSection.TAIL, subscribeResult::add);
        var itineraries = List.of(i1, i2, i3);

        var processedList = DeletionFlaggerTestHelper.process(itineraries, subject);

        assertEquals(toStr(List.of(i3)), toStr(subscribeResult));
        assertEquals(toStr(List.of(i1, i2)), toStr(processedList));
    }

    @Test
    public void testCropHeadAndSubscribe() {
        var subscribeResult = new ArrayList<Itinerary>();
        var subject = new MaxLimitFilter("Test", 1, ListSection.HEAD, subscribeResult::add);
        var itineraries = List.of(i1, i2, i3);

        var processedList = DeletionFlaggerTestHelper.process(itineraries, subject);

        assertEquals(toStr(List.of(i2)), toStr(subscribeResult));
        assertEquals(toStr(List.of(i3)), toStr(processedList));
    }
}