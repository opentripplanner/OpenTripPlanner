package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;

import org.junit.Test;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccessStopArrivalTest {

    private static final int ALIGHT_STOP = 100;
    private static final int DEPARTURE_TIME = 8 * 60 * 60;
    private static final int LEG_DURATION = 10 * 60;
    private static final int ALIGHT_TIME = DEPARTURE_TIME + LEG_DURATION;
    private static final int COST = 500;

    private final AccessStopArrival<RaptorTripSchedule> subject = new AccessStopArrival<>(
        ALIGHT_STOP,
        DEPARTURE_TIME,
        LEG_DURATION,
        COST,
        null
    );



    @Test
    public void arrivedByAccessLeg() {
        assertTrue(subject.arrivedByAccessLeg());
        assertFalse(subject.arrivedByTransit());
        assertFalse(subject.arrivedByTransfer());
    }

    @Test
    public void stop() {
        assertEquals(ALIGHT_STOP, subject.stop());
    }

    @Test
    public void arrivalTime() {
        assertEquals(ALIGHT_TIME, subject.arrivalTime());
    }

    @Test
    public void departureTime() {
        assertEquals(DEPARTURE_TIME, subject.departureTime());
    }

    @Test
    public void cost() {
        assertEquals(COST, subject.cost());
    }

    @Test
    public void round() {
        assertEquals(0, subject.round());
    }

    @Test
    public void travelDuration() {
        assertEquals(LEG_DURATION, subject.travelDuration());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test(expected = IllegalStateException.class)
    public void equalsThrowsExceptionByDesign() {
        subject.equals(null);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test(expected = IllegalStateException.class)
    public void hashCodeThrowsExceptionByDesign() {
        subject.hashCode();
    }

    @Test
    public void testToString() {
        assertEquals(
                "AccessStopArrival { Rnd: 0, Stop: 100, Time: 8:10 (8:00), Cost: 500 }",
                subject.toString()
        );
    }
}