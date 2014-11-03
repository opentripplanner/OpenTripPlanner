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

import org.mapdb.Fun;

import java.io.IOException;
import java.io.Serializable;

public class StopTime extends Entity implements Serializable {

    public String trip_id;
    public int    arrival_time;
    public int    departure_time;
    public String stop_id;
    public int    stop_sequence;
    public String stop_headsign;
    public int    pickup_type;
    public int    drop_off_type;
    public double shape_dist_traveled;

    @Override
    public Fun.Tuple2 getKey() {
        return new Fun.Tuple2(trip_id, stop_sequence);
    }

    public static class Factory extends Entity.Factory<StopTime> {

        public Factory() {
            tableName = "stop_times";
            requiredColumns = new String[] {"trip_id", "stop_sequence"};
        }

        @Override
        public StopTime fromCsv() throws IOException {
            StopTime st = new StopTime();
            st.trip_id        = getStringField("trip_id", true);
            st.arrival_time   = getTimeField("arrival_time");
            st.departure_time = getTimeField("departure_time");
            st.stop_id        = getStringField("stop_id", true);
            st.stop_sequence  = getIntField("stop_sequence", true);
            st.stop_headsign  = getStringField("stop_headsign", false);
            st.pickup_type    = getIntField("pickup_type", false);
            st.drop_off_type  = getIntField("drop_off_type", false);
            st.shape_dist_traveled = getDoubleField("shape_dist_traveled", false);
            return st;
        }

    }


}
