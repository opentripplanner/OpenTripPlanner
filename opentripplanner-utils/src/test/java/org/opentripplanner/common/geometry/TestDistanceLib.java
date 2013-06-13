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

    public void testPointToLineStringFastDistance() {
        // Note: the meridian length of 1 degree of latitude on the sphere is around 111.2 km
        DistanceLibrary dlib = new SphericalDistanceLibrary();
        runOneTestPointToLineStringFastDistance(dlib, 0, 0, 45, 0, 44.9, 0, 45.1, 0);
        runOneTestPointToLineStringFastDistance(dlib, 0, 0, 45, 0, 45, -0.1, 45, 0.1);

        // Mid-range lat
        runOneTestPointToLineStringFastDistance(dlib, 7862, 7863, 45, 0.1, 44.9, 0, 45.1, 0);
        runOneTestPointToLineStringFastDistance(dlib, 11119, 11120, 45.1, 0.0, 45, -0.1, 45, 0.1);
        // Equator
        runOneTestPointToLineStringFastDistance(dlib, 11119, 11120, 0, 0.1, -0.1, 0, 0.1, 0);
        runOneTestPointToLineStringFastDistance(dlib, 11119, 11120, 0.1, 0.0, 0, -0.1, 0, 0.1);

        runOneTestPointToLineStringFastDistance(dlib, 0, 0, 45.1, 0.1, 44.9, -0.1, 45.1, 0.1);
        runOneTestPointToLineStringFastDistance(dlib, 12854, 12855, 44.9, 0.1, 44.9, -0.1, 45.1,
                0.1);

        // Test corners
        runOneTestPointToLineStringFastDistance(dlib, 1361, 1362, 44.99, 0.01, 45, -0.1, 45, 0,
                45.1, 0);
        runOneTestPointToLineStringFastDistance(dlib, 1111, 1112, 44.99, -0.05, 45, -0.1, 45, 0,
                45.1, 0);
        /*
         * Note: the two following case do not have the exact same distance as we project on point
         * location and their latitude differ a bit.
         */
        runOneTestPointToLineStringFastDistance(dlib, 786, 787, 45.01, -0.01, 45, -0.1, 45, 0,
                45.1, 0);
        runOneTestPointToLineStringFastDistance(dlib, 785, 786, 45.05, -0.01, 45, -0.1, 45, 0,
                45.1, 0);
    }

    private void runOneTestPointToLineStringFastDistance(DistanceLibrary dlib, double dMin,
            double dMax, double lat, double lon, double... latlon) {
        double dist = dlib.fastDistance(makeCoordinate(lat, lon), makeLineString(latlon));
        System.out.println("dist=" + dist + ", interval=[" + dMin + "," + dMax + "]");
        assertTrue(dist >= dMin);
        assertTrue(dist <= dMax);
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