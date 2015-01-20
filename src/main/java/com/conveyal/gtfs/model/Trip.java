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
import java.util.Map;

public class Trip extends Entity {

    public Route  route;
    public Service service;
    public String trip_id;
    public String trip_headsign;
    public String trip_short_name;
    public int    direction_id;
    public String block_id;
    public Map<Integer, Shape>  shape_points;
    public String shape_id;
    public int    bikes_allowed;
    public int    wheelchair_accessible;

    public static class Loader extends Entity.Loader<Trip> {

        public Loader(GTFSFeed feed) {
            super(feed, "trips");
        }

        @Override
        public void loadOneRow() throws IOException {
            Trip t = new Trip();
            t.route           = getRefField("route_id", true, feed.routes);
            t.service         = getRefField("service_id", true, feed.services); // TODO calendar is special case, join tables
            t.trip_id         = getStringField("trip_id", true);
            t.trip_headsign   = getStringField("trip_headsign", false);
            t.trip_short_name = getStringField("trip_short_name", false);
            t.direction_id    = getIntField("direction_id", false, 0, 1);
            t.block_id        = getStringField("block_id", false); // make a blocks multimap
            t.shape_points    = getRefField("shape_id", false, feed.shapes);
            t.shape_id        = getStringField("shape_id", false);
            t.bikes_allowed   = getIntField("bikes_allowed", false, 0, 2);
            t.wheelchair_accessible = getIntField("wheelchair_accessible", false, 0, 2);
            t.feed = feed;
            feed.trips.put(t.trip_id, t);
        }

    }

    public static class Writer extends Entity.Writer<Trip> {
        public Writer (GTFSFeed feed) {
            super(feed, "trips");
        }

        @Override
        protected void writeHeaders() throws IOException {
            // TODO: export shapes
            writer.writeRecord(new String[] {"route_id", "trip_id", "trip_headsign", "trip_short_name", "direction_id", "block_id",
                    "shape_id", "bikes_allowed", "wheelchair_accessible", "service_id"});
        }

        @Override
        protected void writeOneRow(Trip t) throws IOException {
            writeStringField(t.route.route_id);
            writeStringField(t.trip_id);
            writeStringField(t.trip_headsign);
            writeStringField(t.trip_short_name);
            writeIntField(t.direction_id);
            writeStringField(t.block_id);
            writeStringField(t.shape_id);
            writeIntField(t.bikes_allowed);
            writeIntField(t.wheelchair_accessible);
            writeStringField(t.service.service_id);
            endRecord();
        }

        @Override
        protected Iterator<Trip> iterator() {
            return feed.trips.values().iterator();
        }


    }

}
