package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ServiceCalendarDateMapperTest {
    private static final ServiceCalendarDate SERVICE_DATE = new ServiceCalendarDate();

    private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

    private static final Integer ID = 45;

    private static final ServiceDate DATE = new ServiceDate(2017, 10, 15);

    private static final int EXCEPTION_TYPE = 2;

    static {
        SERVICE_DATE.setId(ID);
        SERVICE_DATE.setDate(DATE);
        SERVICE_DATE.setExceptionType(EXCEPTION_TYPE);
        SERVICE_DATE.setServiceId(AGENCY_AND_ID);
    }

    private ServiceCalendarDateMapper subject = new ServiceCalendarDateMapper();

    @Test
    public void testMapCollection() throws Exception {
        assertNull(null, subject.map((Collection<ServiceCalendarDate>) null));
        assertTrue(subject.map(Collections.emptyList()).isEmpty());
        assertEquals(1, subject.map(Collections.singleton(SERVICE_DATE)).size());
    }

    @Test
    public void testMap() throws Exception {
        org.opentripplanner.model.ServiceCalendarDate result = subject.map(SERVICE_DATE);

        assertEquals(DATE.getAsString(), result.getDate().getAsString());
        assertEquals(EXCEPTION_TYPE, result.getExceptionType());
        assertEquals("A_1", result.getServiceId().toString());

    }

    @Test
    public void testMapWithNulls() throws Exception {
        ServiceCalendarDate input = new ServiceCalendarDate();
        org.opentripplanner.model.ServiceCalendarDate result = subject.map(input);

        assertNull(result.getDate());
        assertEquals(0, result.getExceptionType());
        assertNull(result.getServiceId());
    }

    /** Mapping the same object twice, should return the the same instance. */
    @Test
    public void testMapCache() throws Exception {
        org.opentripplanner.model.ServiceCalendarDate result1 = subject.map(SERVICE_DATE);
        org.opentripplanner.model.ServiceCalendarDate result2 = subject.map(SERVICE_DATE);

        assertTrue(result1 == result2);
    }
}