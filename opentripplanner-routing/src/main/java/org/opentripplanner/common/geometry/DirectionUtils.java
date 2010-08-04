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

import org.geotools.referencing.GeodeticCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class DirectionUtils {

    public static DirectionUtils instance;
    /* this is used to calculate angles on a sphere */
    private GeodeticCalculator geodeticCalculator;

    private DirectionUtils() {
        geodeticCalculator = new GeodeticCalculator();
    }

    private static DirectionUtils getInstance() {
        if (instance == null) {
            instance = new DirectionUtils();
        }
        return instance;
    }

    /**
     * Computes the angle of the last segment of a LineString or MultiLineString
     *
     * @param geometry
     *            a LineString or a MultiLineString
     * @return
     */
    public static double getLastAngle(Geometry geometry) {
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

        DirectionUtils utils = getInstance();
        utils.geodeticCalculator.setStartingGeographicPoint(coord0.x, coord0.y);
        utils.geodeticCalculator.setDestinationGeographicPoint(coord1.x, coord1.y);
        return utils.geodeticCalculator.getAzimuth() * Math.PI / 180;
    }

    /**
     * Computes the angle of the first segment of a LineString or MultiLineString
     *
     * @param geometry
     *            a LineString or a MultiLineString
     * @return
     */
    public static double getFirstAngle(Geometry geometry) {
        LineString line;
        if (geometry instanceof MultiLineString) {
            line = (LineString) geometry.getGeometryN(0);
        } else {
            assert geometry instanceof LineString;
            line = (LineString) geometry;
        }

        Coordinate coord0 = line.getCoordinateN(0);
        Coordinate coord1 = line.getCoordinateN(1);

        DirectionUtils utils = getInstance();
        utils.geodeticCalculator.setStartingGeographicPoint(coord0.x, coord0.y);
        utils.geodeticCalculator.setDestinationGeographicPoint(coord1.x, coord1.y);
        return utils.geodeticCalculator.getAzimuth() * Math.PI / 180;
    }
}
