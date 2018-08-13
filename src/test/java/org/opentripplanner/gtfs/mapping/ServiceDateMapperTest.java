package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opentripplanner.gtfs.mapping.ServiceDateMapper.mapServiceDate;

public class ServiceDateMapperTest {

    @Test
    public void testMapServiceDate() throws Exception {
        ServiceDate input = new ServiceDate(2017, 10, 3);

        org.opentripplanner.model.calendar.ServiceDate result = mapServiceDate(input);

        assertEquals(2017, result.getYear());
        assertEquals(10, result.getMonth());
        assertEquals(3, result.getDay());
    }

    @Test
    public void testMapServiceDateNullRef() throws Exception {
        assertNull(mapServiceDate(null));
    }
}