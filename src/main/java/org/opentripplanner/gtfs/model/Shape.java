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

public class Shape {
    final public String shape_id;
    final public String shape_pt_lat;
    final public String shape_pt_lon;
    final public String shape_pt_sequence;
    final public String shape_dist_traveled;

    public Shape(Map<String, String> row) {
        shape_id = row.get("shape_id");
        shape_pt_lat = row.get("shape_pt_lat");
        shape_pt_lon = row.get("shape_pt_lon");
        shape_pt_sequence = row.get("shape_pt_sequence");
        shape_dist_traveled = row.get("shape_dist_traveled");
    }
}
