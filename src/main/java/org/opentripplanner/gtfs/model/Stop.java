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

public class Stop {
    final public String stop_id;
    final public String stop_code;
    final public String stop_name;
    final public String stop_desc;
    final public String stop_lat;
    final public String stop_lon;
    final public String zone_id;
    final public String stop_url;
    final public String location_type;
    final public String parent_station;
    final public String stop_timezone;
    final public String wheelchair_boarding;

    public Stop(Map<String, String> row) {
        stop_id = row.get("stop_id");
        stop_code = row.get("stop_code");
        stop_name = row.get("stop_name");
        stop_desc = row.get("stop_desc");
        stop_lat = row.get("stop_lat");
        stop_lon = row.get("stop_lon");
        zone_id = row.get("zone_id");
        stop_url = row.get("stop_url");
        location_type = row.get("location_type");
        parent_station = row.get("parent_station");
        stop_timezone = row.get("stop_timezone");
        wheelchair_boarding = row.get("wheelchair_boarding");
    }
}
