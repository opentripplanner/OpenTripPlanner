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

public class Transfer {
    final public String from_stop_id;
    final public String to_stop_id;
    final public String transfer_type;
    final public String min_transfer_time;

    public Transfer(Map<String, String> row) {
        from_stop_id = row.get("from_stop_id");
        to_stop_id = row.get("to_stop_id");
        transfer_type = row.get("transfer_type");
        min_transfer_time = row.get("min_transfer_time");
    }
}
