package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.model.calendar.ServiceDate.MAX_DATE;
import static org.opentripplanner.model.calendar.ServiceDate.MIN_DATE;

public class ServiceCalendarMapperTest {
    private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

    private static final Integer ID = 45;

    private static final int MONDAY = 1;

    private static final int TUESDAY = 2;

    private static final int WEDNESDAY = 3;

    private static final int THURSDAY = 4;

    private static final int FRIDAY = 5;

    private static final int SATURDAY = 6;

    private static final int SUNDAY = 7;

    private static final ServiceDate START_DATE = new ServiceDate(2017, 10, 17);

    private static final ServiceDate END_DATE = new ServiceDate(2018, 1, 2);

    private static final ServiceCalendar CALENDAR = new ServiceCalendar();

    static {
        CALENDAR.setId(ID);
        CALENDAR.setServiceId(AGENCY_AND_ID);
        CALENDAR.setMonday(MONDAY);
        CALENDAR.setTuesday(TUESDAY);
        CALENDAR.setWednesday(WEDNESDAY);
        CALENDAR.setThursday(THURSDAY);
        CALENDAR.setFriday(FRIDAY);
        CALENDAR.setSaturday(SATURDAY);
        CALENDAR.setSunday(SUNDAY);
        CALENDAR.setStartDate(START_DATE);
        CALENDAR.setEndDate(END_DATE);
    }

    private ServiceCalendarMapper subject = new ServiceCalendarMapper();

    @Test
    public void testMapCollection() {
        assertNull(null, subject.map((Collection<ServiceCalendar>) null));
        assertTrue(subject.map(Collections.emptyList()).isEmpty());
        assertEquals(1, subject.map(Collections.singleton(CALENDAR)).size());
    }

    @Test
    public void testMap() {
        org.opentripplanner.model.calendar.ServiceCalendar result = subject.map(CALENDAR);

        assertEquals("A:1", result.getServiceId().toString());
        assertEquals(MONDAY, result.getMonday());
        assertEquals(TUESDAY, result.getTuesday());
        assertEquals(WEDNESDAY, result.getWednesday());
        assertEquals(THURSDAY, result.getThursday());
        assertEquals(FRIDAY, result.getFriday());
        assertEquals(SATURDAY, result.getSaturday());
        assertEquals(SUNDAY, result.getSunday());
        assertEquals(START_DATE.getAsString(), result.getPeriod().getStart().asCompactString());
        assertEquals(END_DATE.getAsString(), result.getPeriod().getEnd().asCompactString());
    }

    @Test
    public void testMapWithNulls() {
        ServiceCalendar input = new ServiceCalendar();
        org.opentripplanner.model.calendar.ServiceCalendar result = subject.map(input);

        assertEquals(0, result.getMonday());
        assertEquals(0, result.getTuesday());
        assertEquals(0, result.getWednesday());
        assertEquals(0, result.getThursday());
        assertEquals(0, result.getFriday());
        assertEquals(0, result.getSaturday());
        assertEquals(0, result.getSunday());
        assertEquals(MIN_DATE, result.getPeriod().getStart());
        assertEquals(MAX_DATE, result.getPeriod().getEnd());
    }

    /** Mapping the same object twice, should return the the same instance. */
    @Test
    public void testMapCache() {
        org.opentripplanner.model.calendar.ServiceCalendar result1 = subject.map(CALENDAR);
        org.opentripplanner.model.calendar.ServiceCalendar result2 = subject.map(CALENDAR);

        assertSame(result1, result2);
    }

}