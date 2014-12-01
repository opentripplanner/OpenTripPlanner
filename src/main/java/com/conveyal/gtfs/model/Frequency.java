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
import java.util.Iterator;

public class Frequency extends Entity {

    public Trip trip;
    public int start_time;
    public int end_time;
    public int headway_secs;
    public int exact_times;

    public static class Loader extends Entity.Loader<Frequency> {

        public Loader(GTFSFeed feed) {
            super(feed, "frequencies");
        }

        @Override
        public void loadOneRow() throws IOException {
            Frequency f = new Frequency();
            f.trip = getRefField("trip_id", true, feed.trips);
            f.start_time = getTimeField("start_time");
            f.end_time = getTimeField("end_time");
            f.headway_secs = getIntField("headway_secs", true, 1, 24 * 60 * 60);
            f.exact_times = getIntField("exact_times", false, 0, 1);
            f.feed = feed;
            feed.frequencies.put(f.trip.trip_id, f); // TODO this should be a multimap
        }

    }

    public static class Writer extends Entity.Writer<Frequency> {
        public Writer (GTFSFeed feed) {
            super(feed, "frequencies");
        }

        @Override
        public void writeHeaders() throws IOException {
            writer.writeRecord(new String[] {"trip_id", "start_time", "end_time", "headway_secs", "exact_times"});
        }

        @Override
        public void writeOneRow(Frequency f) throws IOException {
            writeStringField(f.trip.trip_id);
            writeTimeField(f.start_time);
            writeTimeField(f.end_time);
            writeIntField(f.headway_secs);
            writeIntField(f.exact_times);
        }

        @Override
        public Iterator<Frequency> iterator() {
            return feed.frequencies.values().iterator();
        }


    }

}
