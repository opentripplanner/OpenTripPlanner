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

public class Transfer extends Entity {

    public Stop from_stop;
    public Stop to_stop;
    public int  transfer_type;
    public int  min_transfer_time;

    public static class Loader extends Entity.Loader<Transfer> {

        public Loader(GTFSFeed feed) {
            super(feed, "transfers");
        }

        @Override
        public void loadOneRow() throws IOException {
            Transfer tr = new Transfer();
            tr.from_stop         = getRefField("from_stop_id", true, feed.stops);
            tr.to_stop           = getRefField("to_stop_id", true, feed.stops);
            tr.transfer_type     = getIntField("transfer_type", true, 0, 3);
            tr.min_transfer_time = getIntField("min_transfer_time", false, 0, Integer.MAX_VALUE);
            tr.feed = feed;
            feed.transfers.put(Long.toString(row), tr);
        }

    }

    public static class Writer extends Entity.Writer<Transfer> {
        public Writer (GTFSFeed feed) {
            super(feed, "transfers");
        }

        @Override
        protected void writeHeaders() throws IOException {
            writer.writeRecord(new String[] {"from_stop_id", "to_stop_id", "transfer_type", "min_transfer_time"});
        }

        @Override
        protected void writeOneRow(Transfer t) throws IOException {
            writeStringField(t.from_stop.stop_id);
            writeStringField(t.to_stop.stop_id);
            writeIntField(t.transfer_type);
            writeIntField(t.min_transfer_time);
            endRecord();
        }

        @Override
        protected Iterator<Transfer> iterator() {
            return feed.transfers.values().iterator();
        }


    }
}
