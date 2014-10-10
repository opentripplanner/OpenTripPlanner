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

public class CalendarDate {
    final public String service_id;
    final public String date;
    final public String exception_type;

    public CalendarDate(Map<String, String> row) {
        service_id = row.get("service_id");
        date = row.get("date");
        exception_type = row.get("exception_type");
    }
}
