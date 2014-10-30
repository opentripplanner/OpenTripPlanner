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

public class Frequency extends Entity {

    public String trip_id;
    public int start_time;
    public int end_time;
    public int headway_secs;
    public int exact_times;

    @Override
    public Object getKey() {
        return trip_id;
    }

    public static class Factory extends Entity.Factory<Frequency> {

        public Factory() {
            tableName = "frequencies";
            requiredColumns = new String[] {"trip_id"};
        }

        @Override
        public Frequency fromCsv() throws IOException {
            Frequency f = new Frequency();
            f.trip_id = getStringField("trip_id", true);
            f.start_time = getTimeField("start_time");
            f.end_time = getTimeField("end_time");
            f.headway_secs = getIntField("headway_secs", true);
            f.exact_times = getIntField("exact_times", false);
            return f;
        }

    }

}
