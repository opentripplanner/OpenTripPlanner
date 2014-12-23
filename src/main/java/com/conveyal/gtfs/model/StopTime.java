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

import org.mapdb.Fun;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;

public class StopTime extends Entity implements Serializable {

    /* StopTime cannot directly reference Trips or Stops because they would be serialized into the MapDB. */
    public final String trip_id;
    public final int    arrival_time;
    public final int    departure_time;
    public final String stop_id;
    public final int    stop_sequence;
    public final String stop_headsign;
    public final int    pickup_type;
    public final int    drop_off_type;
    public final double shape_dist_traveled;

    // we have a constructor because StopTimes need to be immutable for use in MapDB
    public StopTime (String trip_id, int arrival_time, int departure_time, String stop_id, int stop_sequence,
            String stop_headsign, int pickup_type, int drop_off_type, double shape_dist_traveled) {
        this.trip_id = trip_id;
        this.arrival_time = arrival_time;
        this.departure_time = departure_time;
        this.stop_id = stop_id;
        this.stop_sequence = stop_sequence;
        this.stop_headsign = stop_headsign;
        this.pickup_type = pickup_type;
        this.drop_off_type = drop_off_type;
        this.shape_dist_traveled = shape_dist_traveled;
    }

    public static class Loader extends Entity.Loader<StopTime> {

        public Loader(GTFSFeed feed) {
            super(feed, "stop_times");
        }

        @Override
        public void loadOneRow() throws IOException {
            String trip_id        = getStringField("trip_id", true);
            int arrival_time   = getTimeField("arrival_time");
            int departure_time = getTimeField("departure_time");
            String stop_id        = getStringField("stop_id", true);
            int stop_sequence  = getIntField("stop_sequence", true, 0, Integer.MAX_VALUE);
            String stop_headsign  = getStringField("stop_headsign", false);
            int pickup_type    = getIntField("pickup_type", false, 0, 3); // TODO add ranges as parameters
            int drop_off_type  = getIntField("drop_off_type", false, 0, 3);
            double shape_dist_traveled = getDoubleField("shape_dist_traveled", false, 0D, Double.MAX_VALUE);
            StopTime st = new StopTime(trip_id, arrival_time, departure_time, stop_id, stop_sequence,
                    stop_headsign, pickup_type, drop_off_type, shape_dist_traveled);
            st.feed = null; // this could circular-serialize the whole feed
            feed.stop_times.put(new Fun.Tuple2(st.trip_id, st.stop_sequence), st);

            /*
              Check referential integrity without storing references. StopTime cannot directly reference Trips or
              Stops because they would be serialized into the MapDB.
             */
            getRefField("trip_id", true, feed.trips);
            getRefField("stop_id", true, feed.stops);
        }

    }

    public static class Writer extends Entity.Writer<StopTime> {
        public Writer (GTFSFeed feed) {
            super(feed, "stop_times");
        }

        @Override
        protected void writeHeaders() throws IOException {
            writer.writeRecord(new String[] {"trip_id", "arrival_time", "departure_time", "stop_id", "stop_sequence", "stop_headsign",
                    "pickup_type", "drop_off_type", "shape_dist_traveled"});
        }

        @Override
        protected void writeOneRow(StopTime st) throws IOException {
            writeStringField(st.trip_id);
            writeTimeField(st.arrival_time);
            writeTimeField(st.departure_time);
            writeStringField(st.stop_id);
            writeIntField(st.stop_sequence);
            writeStringField(st.stop_headsign);
            writeIntField(st.pickup_type);
            writeIntField(st.drop_off_type);
            writeDoubleField(st.shape_dist_traveled);
            endRecord();
        }

        @Override
        protected Iterator<StopTime> iterator() {
            return feed.stop_times.values().iterator();
        }


    }
}
