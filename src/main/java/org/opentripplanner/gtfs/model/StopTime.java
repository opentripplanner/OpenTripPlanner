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

import org.mapdb.Fun;

import java.io.IOException;
import java.util.Map;

public class StopTime extends Entity {

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
            st.trip_id        = getStrField("trip_id");
            st.arrival_time   = getIntField("arrival_time");
            st.departure_time = getIntField("departure_time");
            st.stop_id        = getStrField("stop_id");
            st.stop_sequence  = getIntField("stop_sequence");
            st.stop_headsign  = getStrField("stop_headsign");
            st.pickup_type    = getIntField("pickup_type");
            st.drop_off_type  = getIntField("drop_off_type");
            st.shape_dist_traveled = getDblField("shape_dist_traveled");
            return st;
        }

    }


}
