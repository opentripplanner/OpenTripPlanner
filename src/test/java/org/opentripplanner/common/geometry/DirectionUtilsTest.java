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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.geotools.referencing.GeodeticCalculator;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;

public class DirectionUtilsTest extends TestCase {

    @Test
    public final void testAzimuth() {

        final int N_RUN = 1000000;

        GeodeticCalculator geodeticCalculator = new GeodeticCalculator();

        Random rand = new Random(42);
        List<Coordinate> from = new ArrayList<>(N_RUN);
        List<Coordinate> to = new ArrayList<>(N_RUN);

        // Add fixed points
        from.add(new Coordinate(0, 45));
        to.add(new Coordinate(0, 45)); // Undefined: 180 deg
        assertTrue(DirectionUtils.getAzimuth(from.get(0), to.get(0)) == 180);

        from.add(new Coordinate(0, 45));
        to.add(new Coordinate(0.1, 45)); // East: 90 deg
        assertTrue(DirectionUtils.getAzimuth(from.get(1), to.get(1)) == 90);

        from.add(new Coordinate(0, 45));
        to.add(new Coordinate(0, 45.1)); // North: 0 deg
        assertTrue(DirectionUtils.getAzimuth(from.get(2), to.get(2)) == 0);

        from.add(new Coordinate(0, 45));
        to.add(new Coordinate(-0.1, 45)); // West: -90 deg
        assertTrue(DirectionUtils.getAzimuth(from.get(3), to.get(3)) == -90);

        from.add(new Coordinate(0, 45));
        to.add(new Coordinate(0, 44.9)); // South: 180 deg
        assertTrue(DirectionUtils.getAzimuth(from.get(4), to.get(4)) == 180);

        for (int i = 0; i < N_RUN; i++) {
            Coordinate a = new Coordinate(rand.nextDouble() * 0.1, 45 + rand.nextDouble() * 0.1);
            Coordinate b = new Coordinate(rand.nextDouble() * 0.1, 45 + rand.nextDouble() * 0.1);
            from.add(a);
            to.add(b);
        }

        double[] exactAzimuths = new double[from.size()];
        double[] approxAzimuths = new double[to.size()];

        long start = System.currentTimeMillis();
        for (int i = 0; i < from.size(); i++) {
            geodeticCalculator.setStartingGeographicPoint(from.get(i).x, from.get(i).y);
            geodeticCalculator.setDestinationGeographicPoint(to.get(i).x, to.get(i).y);
            exactAzimuths[i] = geodeticCalculator.getAzimuth();
        }
        long exactTimeMs = System.currentTimeMillis() - start;
        System.out.println("GeodeticCalculator exact azimuth: " + exactTimeMs + "ms for " + N_RUN
                + " computations.");

        start = System.currentTimeMillis();
        for (int i = 0; i < from.size(); i++) {
            approxAzimuths[i] = DirectionUtils.getAzimuth(from.get(i), to.get(i));
        }
        long approxTimeMs = System.currentTimeMillis() - start;
        System.out.println("UtilsDistance approx azimuth: " + approxTimeMs + "ms for " + N_RUN
                + " computations.");

        double maxError = 0.0;
        for (int i = 0; i < exactAzimuths.length; i++) {
            double error = (exactAzimuths[i] - approxAzimuths[i]); // Degrees
            if (error > 360)
                error -= 360;
            if (error < -360)
                error += 360;
            if (error > maxError)
                maxError = error;
        }
        System.out.println("Max error in azimuth: " + maxError + " degrees.");
        assertTrue(maxError < 0.15);
    }
}
