/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
    public void testMapCollection() throws Exception {
        assertNull(null, subject.map((Collection<ServiceCalendar>) null));
        assertTrue(subject.map(Collections.emptyList()).isEmpty());
        assertEquals(1, subject.map(Collections.singleton(CALENDAR)).size());
    }

    @Test
    public void testMap() throws Exception {
        org.opentripplanner.model.ServiceCalendar result = subject.map(CALENDAR);

        assertEquals(ID, result.getId());
        assertEquals("A_1", result.getServiceId().toString());
        assertEquals(MONDAY, result.getMonday());
        assertEquals(TUESDAY, result.getTuesday());
        assertEquals(WEDNESDAY, result.getWednesday());
        assertEquals(THURSDAY, result.getThursday());
        assertEquals(FRIDAY, result.getFriday());
        assertEquals(SATURDAY, result.getSaturday());
        assertEquals(SUNDAY, result.getSunday());
        assertEquals(START_DATE.getAsString(), result.getStartDate().getAsString());
        assertEquals(END_DATE.getAsString(), result.getEndDate().getAsString());
    }

    @Test
    public void testMapWithNulls() throws Exception {
        ServiceCalendar input = new ServiceCalendar();
        org.opentripplanner.model.ServiceCalendar result = subject.map(input);

        assertEquals(Integer.valueOf(0), result.getId());
        assertEquals(0, result.getMonday());
        assertEquals(0, result.getTuesday());
        assertEquals(0, result.getWednesday());
        assertEquals(0, result.getThursday());
        assertEquals(0, result.getFriday());
        assertEquals(0, result.getSaturday());
        assertEquals(0, result.getSunday());
        assertNull(result.getStartDate());
        assertNull(result.getEndDate());
    }

    /** Mapping the same object twice, should return the the same instance. */
    @Test
    public void testMapCache() throws Exception {
        org.opentripplanner.model.ServiceCalendar result1 = subject.map(CALENDAR);
        org.opentripplanner.model.ServiceCalendar result2 = subject.map(CALENDAR);

        assertTrue(result1 == result2);
    }

}