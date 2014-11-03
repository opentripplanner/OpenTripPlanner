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

public class Calendar extends Entity {

    public String service_id;
    public int monday;
    public int tuesday;
    public int wednesday;
    public int thursday;
    public int friday;
    public int saturday;
    public int sunday;
    public int start_date;
    public int end_date;

    @Override
    public String getKey() {
        return ""; // TODO auto-increment
    }

    public static class Factory extends Entity.Factory<Calendar> {

        public Factory() {
            tableName = "calendars";
            requiredColumns = new String[] {"service_id"};
        }

        @Override
        public Calendar fromCsv() throws IOException {
            Calendar c = new Calendar();
            c.service_id = getStringField("service_id", true);
            c.monday     = getIntField("monday", true);
            c.tuesday    = getIntField("tuesday", true);
            c.wednesday  = getIntField("wednesday", true);
            c.thursday   = getIntField("thursday", true);
            c.friday     = getIntField("friday", true);
            c.saturday   = getIntField("saturday", true);
            c.sunday     = getIntField("sunday", true);
            c.start_date = getIntField("start_date", true);
            c.end_date   = getIntField("end_date", true);
            return c;
        }

    }

}
