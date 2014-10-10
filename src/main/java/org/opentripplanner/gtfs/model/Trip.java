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

import java.util.Map;

public class Trip {
    final public String route_id;
    final public String service_id;
    final public String trip_id;
    final public String trip_headsign;
    final public String trip_short_name;
    final public String direction_id;
    final public String block_id;
    final public String shape_id;
    final public String wheelchair_accessible;
    final public String bikes_allowed;

    public Trip(Map<String, String> row) {
        route_id = row.get("route_id");
        service_id = row.get("service_id");
        trip_id = row.get("trip_id");
        trip_headsign = row.get("trip_headsign");
        trip_short_name = row.get("trip_short_name");
        direction_id = row.get("direction_id");
        block_id = row.get("block_id");
        shape_id = row.get("shape_id");
        wheelchair_accessible = row.get("wheelchair_accessible");
        bikes_allowed = row.get("bikes_allowed");
    }
}
