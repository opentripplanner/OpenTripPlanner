package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.poole.openinghoursparser.OpeningHoursParseException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TimeRestrictionWithTimeSpanTest {

    @Test
    public void testAlwaysOpen() throws OpeningHoursParseException {
        var testee = TimeRestrictionWithTimeSpan.of(OsmOpeningHours.parseFromOsm("24/7"), 60);
        var time = LocalDateTime.of(2021, 5, 20, 12, 0, 0);
        assertTrue(testee.isTraverseableAt(time));
        assertEquals(time, testee.earliestDepartureTime(time).get());
        assertEquals(time, testee.latestArrivalTime(time).get());
    }

    @Test
    public void testClosedAtStart() throws OpeningHoursParseException {
        var testee = TimeRestrictionWithTimeSpan.of(OsmOpeningHours.parseFromOsm("Mo-Su 12:01-14:00"), 120);
        var time = LocalDateTime.of(2021, 5, 20, 12, 0, 0);
        var edt = LocalDateTime.of(2021, 5, 20, 12, 1, 0);
        var lat = LocalDateTime.of(2021, 5, 19, 13, 58, 0);
        assertFalse(testee.isTraverseableAt(time));
        assertEquals(edt, testee.earliestDepartureTime(time).get());
        assertEquals(lat, testee.latestArrivalTime(time).get());
    }

    @Test
    public void testClosedAtEnd() throws OpeningHoursParseException {
        var testee = TimeRestrictionWithTimeSpan.of(OsmOpeningHours.parseFromOsm("Mo-Su 10:00-12:00"), 60);
        var time = LocalDateTime.of(2021, 5, 20, 12, 0, 0);
        var edt = LocalDateTime.of(2021, 5, 21, 10, 0, 0);
        var lat = LocalDateTime.of(2021, 5, 20, 11, 59, 0);
        assertFalse(testee.isTraverseableAt(time));
        assertEquals(edt, testee.earliestDepartureTime(time).get());
        assertEquals(lat, testee.latestArrivalTime(time).get());
    }

    @Test
    public void testClosed() throws OpeningHoursParseException {
        var testee = TimeRestrictionWithTimeSpan.of(OsmOpeningHours.parseFromOsm("Mo-Su 11:00-13:00"), 60);
        var time = LocalDateTime.of(2021, 5, 20, 10, 0, 0);
        var edt = LocalDateTime.of(2021, 5, 20, 11, 0, 0);
        var lat = LocalDateTime.of(2021, 5, 19, 12, 59, 0);
        assertFalse(testee.isTraverseableAt(time));
        assertEquals(edt, testee.earliestDepartureTime(time).get());
        assertEquals(lat, testee.latestArrivalTime(time).get());
    }
}