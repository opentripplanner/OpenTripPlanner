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
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import junit.framework.TestCase;

public class TestDistanceLib extends TestCase {

    public void testMetersToDegree() {
        // Note: 111194.926644559 is 1 degree at the equator, given the earth radius used in the lib
        double degree;
        degree = SphericalDistanceLibrary.metersToDegrees(111194.926644559);
        assertTrue(Math.abs(degree - 1.0) < 1e-5);
        degree = SphericalDistanceLibrary.metersToLonDegrees(111194.926644559, 0);
        assertTrue(Math.abs(degree - 1.0 / Math.cos(Math.toRadians(1))) < 1e-5);
        degree = SphericalDistanceLibrary.metersToLonDegrees(111194.926644559, 1.0);
        assertTrue(Math.abs(degree - 1.0 / Math.cos(Math.toRadians(2))) < 1e-5);
        degree = SphericalDistanceLibrary.metersToLonDegrees(111194.926644559, -1.0);
        assertTrue(Math.abs(degree - 1.0 / Math.cos(Math.toRadians(2))) < 1e-5);
        degree = SphericalDistanceLibrary.metersToLonDegrees(111194.926644559, 45.0);
        assertTrue(Math.abs(degree - 1.0 / Math.cos(Math.toRadians(46))) < 1e-5);
        degree = SphericalDistanceLibrary.metersToLonDegrees(111194.926644559, -45.0);
        assertTrue(Math.abs(degree - 1.0 / Math.cos(Math.toRadians(46))) < 1e-5);
        // Further north, solutions get degenerated.
        degree = SphericalDistanceLibrary.metersToLonDegrees(111194.926644559, 80);
        assertTrue(Math.abs(degree - 1.0 / Math.cos(Math.toRadians(81))) < 1e-4);
        degree = SphericalDistanceLibrary.metersToLonDegrees(111.194926, 45);
        assertTrue(Math.abs(degree - 1.0 / Math.cos(Math.toRadians(44.999)) / 1000) < 1e-5);
    }

    public void testPointToLineStringFastDistance() {
        // Note: the meridian length of 1 degree of latitude on the sphere is around 111.2 km
        runOneTestPointToLineStringFastDistance(0, 0, 45, 0, 44.9, 0, 45.1, 0);
        runOneTestPointToLineStringFastDistance(0, 0, 45, 0, 45, -0.1, 45, 0.1);

        // Mid-range lat
        runOneTestPointToLineStringFastDistance(7862, 7863, 45, 0.1, 44.9, 0, 45.1, 0);
        runOneTestPointToLineStringFastDistance(11119, 11120, 45.1, 0.0, 45, -0.1, 45, 0.1);
        // Equator
        runOneTestPointToLineStringFastDistance(11119, 11120, 0, 0.1, -0.1, 0, 0.1, 0);
        runOneTestPointToLineStringFastDistance(11119, 11120, 0.1, 0.0, 0, -0.1, 0, 0.1);

        runOneTestPointToLineStringFastDistance(0, 0, 45.1, 0.1, 44.9, -0.1, 45.1, 0.1);
        runOneTestPointToLineStringFastDistance(12854, 12855, 44.9, 0.1, 44.9, -0.1, 45.1,
                0.1);

        // Test corners
        runOneTestPointToLineStringFastDistance(1361, 1362, 44.99, 0.01, 45, -0.1, 45, 0,
                45.1, 0);
        runOneTestPointToLineStringFastDistance(1111, 1112, 44.99, -0.05, 45, -0.1, 45, 0,
                45.1, 0);
        /*
         * Note: the two following case do not have the exact same distance as we project on point
         * location and their latitude differ a bit.
         */
        runOneTestPointToLineStringFastDistance(786, 787, 45.01, -0.01, 45, -0.1, 45, 0,
                45.1, 0);
        runOneTestPointToLineStringFastDistance(785, 786, 45.05, -0.01, 45, -0.1, 45, 0,
                45.1, 0);
    }

    private void runOneTestPointToLineStringFastDistance(
            double dMin, double dMax, double lat, double lon, double... latlon) {
        double dist = SphericalDistanceLibrary.fastDistance(makeCoordinate(lat, lon), makeLineString(latlon));
        System.out.println("dist=" + dist + ", interval=[" + dMin + "," + dMax + "]");
        assertTrue(dist >= dMin);
        assertTrue(dist <= dMax);
    }

    public void testLineStringFastLenght() {
        // Note: the meridian length of 1 degree of latitude on the sphere is around 111.2 km
        // a ~= 111.2 km
        double a = runOneTestLineStringFastLength(11119, 11120, 45, 0, 45.1, 0);
        // b ~= a . cos(45)
        double b = runOneTestLineStringFastLength(7862, 7863, 45, 0, 45, 0.1);
        // c^2 ~= a^2 + b^2
        double c = runOneTestLineStringFastLength(13614, 13615, 45, 0, 45.1, 0.1);
        // d ~= a + b
        double d = runOneTestLineStringFastLength(18975, 18976, 45, 0, 45.1, 0, 45.1, 0.1);
        // fast, but imprecise: error is less than 10 meters for a distance of ~20 kms
        assertTrue(Math.abs(b - a * Math.cos(Math.toRadians(45))) < 1.0);
        assertTrue(Math.abs(c - Math.sqrt(a * a + b * b)) < 10.0);
        assertTrue(Math.abs(d - (a + b)) < 10.0);
    }

    private double runOneTestLineStringFastLength(double dMin, double dMax, double... latlon) {
        double dist = SphericalDistanceLibrary.fastLength(makeLineString(latlon));
        System.out.println("dist=" + dist + ", interval=[" + dMin + "," + dMax + "]");
        assertTrue(dist >= dMin);
        assertTrue(dist <= dMax);
        return dist;
    }

    private Coordinate makeCoordinate(double lat, double lon) {
        return new Coordinate(lon, lat);
    }

    private LineString makeLineString(double... latlon) {
        assertTrue(latlon.length % 2 == 0);
        Coordinate[] coords = new Coordinate[latlon.length / 2];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = new Coordinate(latlon[i * 2 + 1], latlon[i * 2]);
        }
        return new GeometryFactory().createLineString(coords);
    }
}