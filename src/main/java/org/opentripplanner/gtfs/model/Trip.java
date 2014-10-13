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
import java.util.Map;

public class Trip extends Entity {

    public String route_id;
    public String service_id;
    public String trip_id;
    public String trip_headsign;
    public String trip_short_name;
    public String direction_id;
    public String block_id;
    public String shape_id;
    public String bikes_allowed;
    public String wheelchair_accessible;

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
            t.route_id        = getStrField("route_id");
            t.service_id      = getStrField("service_id");
            t.trip_id         = getStrField("trip_id");
            t.trip_headsign   = getStrField("trip_headsign");
            t.trip_short_name = getStrField("trip_short_name");
            t.direction_id    = getStrField("direction_id");
            t.block_id        = getStrField("block_id");
            t.shape_id        = getStrField("shape_id");
            t.bikes_allowed   = getStrField("bikes_allowed");
            t.wheelchair_accessible = getStrField("wheelchair_accessible");
            return t;
        }

    }

}
