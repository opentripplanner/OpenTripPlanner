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

package com.conveyal.gtfs.model;

import java.io.IOException;

public class CalendarDate extends Entity {

    public String service_id;
    public String date;
    public int    exception_type;

    @Override
    public String getKey() {
        return ""; // TODO auto-increment, list not map type (overload load function to take list types)
    }

    public static class Factory extends Entity.Factory<CalendarDate> {

        public Factory() {
            tableName = "calendar_dates";
            requiredColumns = new String[] {"service_id", "date", "exception_type"};
        }

        @Override
        public CalendarDate fromCsv() throws IOException {
            CalendarDate cd = new CalendarDate();
            cd.service_id  = getStringField("service_id", true);
            cd.date = getStringField("date", true);
            cd.exception_type = getIntField("exception_type", true);
            return cd;
        }

    }

}
