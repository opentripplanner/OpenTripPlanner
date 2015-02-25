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

import org.apache.commons.math3.util.FastMath;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class DirectionUtils {

    public static DirectionUtils instance;

    private DirectionUtils() {
    }

    /**
     * Returns the approximate azimuth from coordinate A to B in decimal degrees clockwise from North,
     * in the range (-180° to +180°). The computation is exact for small delta between A and B.
     */
    public static synchronized double getAzimuth(Coordinate a, Coordinate b) {
        double cosLat = FastMath.cos(FastMath.toRadians((a.y + b.y) / 2.0)); 
        double dY = (b.y - a.y); // in degrees, we do not care about the units
        double dX = (b.x - a.x) * cosLat; // same
        if (Math.abs(dX) < 1e-10 && Math.abs(dY) < 1e-10)
            return 180;
        double az = FastMath.toDegrees(FastMath.atan2(dX, dY));
        return az;
    }
    
    /**
     * Computes the angle of the last segment of a LineString or MultiLineString in radians clockwise from North
     * in the range (-PI, PI).
     * @param geometry a LineString or a MultiLineString
     */
    public static synchronized double getLastAngle(Geometry geometry) {
        LineString line;
        if (geometry instanceof MultiLineString) {
            line = (LineString) geometry.getGeometryN(geometry.getNumGeometries() - 1);
        } else {
            assert geometry instanceof LineString;
            line = (LineString) geometry;
        }
        int numPoints = line.getNumPoints();
        Coordinate coord0 = line.getCoordinateN(numPoints - 2);
        Coordinate coord1 = line.getCoordinateN(numPoints - 1);
        int i = numPoints - 3;
        int minDistance = 10;  // Meters        
        while (SphericalDistanceLibrary.fastDistance(coord0, coord1) < minDistance && i >= 0) {
            coord0 = line.getCoordinateN(i--);
        }

        double az = getAzimuth(coord0, coord1);
        return az * Math.PI / 180;
    }

    /**
     * Computes the angle of the first segment of a LineString or MultiLineString in radians clockwise from North
     * in the range (-PI, PI).
     * @param geometry a LineString or a MultiLineString
     */
    public static synchronized double getFirstAngle(Geometry geometry) {
        LineString line;
        if (geometry instanceof MultiLineString) {
            line = (LineString) geometry.getGeometryN(0);
        } else {
            assert geometry instanceof LineString;
            line = (LineString) geometry;
        }

        Coordinate coord0 = line.getCoordinateN(0);
        Coordinate coord1 = line.getCoordinateN(1);
        int i = 2;
        int minDistance = 10;  // Meters 
        while (SphericalDistanceLibrary.fastDistance(coord0, coord1) < minDistance
                && i < line.getNumPoints()) {
            coord1 = line.getCoordinateN(i++);
        }

        double az = getAzimuth(coord0, coord1);
        return az * Math.PI / 180;
    }

}
