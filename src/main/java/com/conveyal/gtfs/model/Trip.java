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

public class Trip extends Entity {

    public String route_id;
    public String service_id;
    public String trip_id;
    public String trip_headsign;
    public String trip_short_name;
    public int    direction_id;
    public String block_id;
    public String shape_id;
    public int    bikes_allowed;
    public int    wheelchair_accessible;

    @Override
    public Object getKey() {
        return trip_id;
    }

    public static class Factory extends Entity.Factory<Trip> {

        public Factory() {
            tableName = "trips";
            requiredColumns = new String[] {"trip_id"};
        }

        @Override
        public Trip fromCsv() throws IOException {
            Trip t = new Trip();
            t.route_id        = getStringField("route_id", true);
            t.service_id      = getStringField("service_id", true);
            t.trip_id         = getStringField("trip_id", true);
            t.trip_headsign   = getStringField("trip_headsign", false);
            t.trip_short_name = getStringField("trip_short_name", false);
            t.direction_id    = getIntField("direction_id", false);
            t.block_id        = getStringField("block_id", false);
            t.shape_id        = getStringField("shape_id", false);
            t.bikes_allowed   = getIntField("bikes_allowed", false);
            t.wheelchair_accessible = getIntField("wheelchair_accessible", false);
            return t;
        }

    }

}
