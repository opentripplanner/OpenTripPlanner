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

public class FareRule {
    final public String fare_id;
    final public String route_id;
    final public String origin_id;
    final public String destination_id;
    final public String contains_id;

    public FareRule(Map<String, String> row) {
        fare_id = row.get("fare_id");
        route_id = row.get("route_id");
        origin_id = row.get("origin_id");
        destination_id = row.get("destination_id");
        contains_id = row.get("contains_id");
    }
}
