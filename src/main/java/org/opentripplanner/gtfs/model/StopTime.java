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

public class StopTime {
    final public String trip_id;
    final public String arrival_time;
    final public String departure_time;
    final public String stop_id;
    final public String stop_sequence;
    final public String stop_headsign;
    final public String pickup_type;
    final public String drop_off_type;
    final public String shape_dist_traveled;

    public StopTime(Map<String, String> row) {
        trip_id = row.get("trip_id");
        arrival_time = row.get("arrival_time");
        departure_time = row.get("departure_time");
        stop_id = row.get("stop_id");
        stop_sequence = row.get("stop_sequence");
        stop_headsign = row.get("stop_headsign");
        pickup_type = row.get("pickup_type");
        drop_off_type = row.get("drop_off_type");
        shape_dist_traveled = row.get("shape_dist_traveled");
    }
}
