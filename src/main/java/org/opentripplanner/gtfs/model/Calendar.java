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

package org.opentripplanner.gtfs.model;

import java.io.IOException;

public class Calendar extends Entity {

    public String service_id;
    public String monday;
    public String tuesday;
    public String wednesday;
    public String thursday;
    public String friday;
    public String saturday;
    public String sunday;
    public String start_date;
    public String end_date;

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
            c.service_id = getStringField("service_id");
            c.monday     = getStringField("monday");
            c.tuesday    = getStringField("tuesday");
            c.wednesday  = getStringField("wednesday");
            c.thursday   = getStringField("thursday");
            c.friday     = getStringField("friday");
            c.saturday   = getStringField("saturday");
            c.sunday     = getStringField("sunday");
            c.start_date = getStringField("start_date");
            c.end_date   = getStringField("end_date");
            return c;
        }

    }

}
