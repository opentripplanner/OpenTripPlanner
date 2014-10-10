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

public class Calendar {
    final public String service_id;
    final public String monday;
    final public String tuesday;
    final public String wednesday;
    final public String thursday;
    final public String friday;
    final public String saturday;
    final public String sunday;
    final public String start_date;
    final public String end_date;

    public Calendar(Map<String, String> row) {
        service_id = row.get("service_id");
        monday = row.get("monday");
        tuesday = row.get("tuesday");
        wednesday = row.get("wednesday");
        thursday = row.get("thursday");
        friday = row.get("friday");
        saturday = row.get("saturday");
        sunday = row.get("sunday");
        start_date = row.get("start_date");
        end_date = row.get("end_date");
    }
}
