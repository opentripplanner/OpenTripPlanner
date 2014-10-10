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

public class Route {
    final public String route_id;
    final public String agency_id;
    final public String route_short_name;
    final public String route_long_name;
    final public String route_desc;
    final public String route_type;
    final public String route_url;
    final public String route_color;
    final public String route_text_color;

    public Route(Map<String, String> row) {
        route_id = row.get("route_id");
        agency_id = row.get("agency_id");
        route_short_name = row.get("route_short_name");
        route_long_name = row.get("route_long_name");
        route_desc = row.get("route_desc");
        route_type = row.get("route_type");
        route_url = row.get("route_url");
        route_color = row.get("route_color");
        route_text_color = row.get("route_text_color");
    }
}
