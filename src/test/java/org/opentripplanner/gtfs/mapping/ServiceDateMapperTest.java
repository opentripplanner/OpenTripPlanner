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