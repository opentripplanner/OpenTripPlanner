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

import com.conveyal.gtfs.GTFSFeed;

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

    public static class Loader extends Entity.Loader<Calendar> {

        public Loader(GTFSFeed feed) {
            super(feed, "calendars");
        }

        @Override
        public void loadOneRow() throws IOException {
            Calendar c = new Calendar();
            c.service_id = getStringField("service_id", true);
            c.monday     = getIntField("monday",     true, 0, 1);
            c.tuesday    = getIntField("tuesday",    true, 0, 1);
            c.wednesday  = getIntField("wednesday",  true, 0, 1);
            c.thursday   = getIntField("thursday",   true, 0, 1);
            c.friday     = getIntField("friday",     true, 0, 1);
            c.saturday   = getIntField("saturday",   true, 0, 1);
            c.sunday     = getIntField("sunday",     true, 0, 1);
            c.start_date = getIntField("start_date", true, 0, 1);
            c.end_date   = getIntField("end_date",   true, 0, 1);

            /* Check referential integrity. TODO service_id can reference either calendar or calendar_dates. */

            feed.calendars.put(c.service_id, c);
        }

    }

}
