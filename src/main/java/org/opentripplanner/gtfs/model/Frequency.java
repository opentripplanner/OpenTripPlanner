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

public class Frequency {
    final public String trip_id;
    final public String start_time;
    final public String end_time;
    final public String headway_secs;
    final public String exact_times;

    public Frequency(Map<String, String> row) {
        trip_id = row.get("trip_id");
        start_time = row.get("start_time");
        end_time = row.get("end_time");
        headway_secs = row.get("headway_secs");
        exact_times = row.get("exact_times");
    }
}
