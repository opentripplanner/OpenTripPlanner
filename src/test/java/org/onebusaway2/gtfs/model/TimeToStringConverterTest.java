/*
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.onebusaway2.gtfs.model;

import org.junit.Test;
import org.onebusaway2.gtfs.model.calendar.TimeToStringConverter;

import static org.junit.Assert.assertEquals;

public class TimeToStringConverterTest {

    private static int T_00_00_00 = 0;
    private static int T_00_00_01 = 1;
    private static int T_00_01_00 = 60;
    private static int T_00_02_01 = 2*60+1;
    private static int T_01_00_00 = 3600;
    private static int T_02_00_01 = 2*3600+1;
    private static int T_26_07_01 = 26*3600+7*60+1;
    private static int N_00_00_01 = -1;
    private static int N_00_01_00 = -60;
    private static int N_00_02_01 = -(2*60+1);
    private static int N_01_00_00 = -3600;
    private static int N_02_00_01 = -(2*3600+1);

    @Test public void toHH_MM_SS() throws Exception {
        assertEquals("00:00:00", TimeToStringConverter.toHH_MM_SS(T_00_00_00));
        assertEquals("00:00:01", TimeToStringConverter.toHH_MM_SS(T_00_00_01));
        assertEquals("00:01:00", TimeToStringConverter.toHH_MM_SS(T_00_01_00));
        assertEquals("00:02:01", TimeToStringConverter.toHH_MM_SS(T_00_02_01));
        assertEquals("01:00:00", TimeToStringConverter.toHH_MM_SS(T_01_00_00));
        assertEquals("02:00:01", TimeToStringConverter.toHH_MM_SS(T_02_00_01));
        assertEquals("26:07:01", TimeToStringConverter.toHH_MM_SS(T_26_07_01));
        assertEquals("-00:00:01", TimeToStringConverter.toHH_MM_SS(N_00_00_01));
        assertEquals("-00:01:00", TimeToStringConverter.toHH_MM_SS(N_00_01_00));
        assertEquals("-00:02:01", TimeToStringConverter.toHH_MM_SS(N_00_02_01));
        assertEquals("-01:00:00", TimeToStringConverter.toHH_MM_SS(N_01_00_00));
        assertEquals("-02:00:01", TimeToStringConverter.toHH_MM_SS(N_02_00_01));
    }

    @Test public void parseHH_MM_SS() {
        assertEquals(T_00_00_00, TimeToStringConverter.parseHH_MM_SS("00:00:00"));
        assertEquals(T_00_00_01, TimeToStringConverter.parseHH_MM_SS("00:00:01"));
        assertEquals(T_00_01_00, TimeToStringConverter.parseHH_MM_SS("00:01:00"));
        assertEquals(T_00_02_01, TimeToStringConverter.parseHH_MM_SS("00:02:01"));
        assertEquals(T_01_00_00, TimeToStringConverter.parseHH_MM_SS("01:00:00"));
        assertEquals(T_02_00_01, TimeToStringConverter.parseHH_MM_SS("02:00:01"));
        assertEquals(T_26_07_01, TimeToStringConverter.parseHH_MM_SS("26:07:01"));
        assertEquals(N_00_00_01, TimeToStringConverter.parseHH_MM_SS("-00:00:01"));
        assertEquals(N_00_01_00, TimeToStringConverter.parseHH_MM_SS("-00:01:00"));
        assertEquals(N_00_02_01, TimeToStringConverter.parseHH_MM_SS("-00:02:01"));
        assertEquals(N_01_00_00, TimeToStringConverter.parseHH_MM_SS("-01:00:00"));
        assertEquals(N_02_00_01, TimeToStringConverter.parseHH_MM_SS("-02:00:01"));
    }

}