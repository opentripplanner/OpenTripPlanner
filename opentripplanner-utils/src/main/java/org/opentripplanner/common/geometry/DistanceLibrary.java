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

package org.opentripplanner.common.geometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public interface DistanceLibrary {

    public abstract double distance(Coordinate from, Coordinate to);

    public abstract double fastDistance(Coordinate from, Coordinate to);

    /**
     * Compute an (approximated) distance between a point and a linestring expressed in standard geographical
     * coordinates (lon, lat in degrees).
     * @param point The coordinates of the point (longitude, latitude degrees).
     * @param lineString The set of points representing the polyline, in the same coordinate system.
     * @return The (approximated) distance, in meters, between the point and the linestring.
     */
    public abstract double fastDistance(Coordinate point, LineString lineString);

    public abstract double distance(double lat1, double lon1, double lat2, double lon2);

    public abstract double fastDistance(double lat1, double lon1, double lat2, double lon2);

    public abstract double distance(double lat1, double lon1, double lat2, double lon2,
            double radius);
    
}