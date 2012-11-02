/**
 Copyright 2012, OpenPlans

 This program is free software: you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 
 ---------------------------------------
 
 These test cases are against the C++ version of VisiLibity's output for the same input. 
 
 */

package org.opentripplanner.visibility;

import java.util.Arrays;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.opentripplanner.visibility.Environment;
import org.opentripplanner.visibility.VLPoint;
import org.opentripplanner.visibility.VLPolygon;
import org.opentripplanner.visibility.VisibilityGraph;

public class TestVisibilityGraph extends TestCase {

    // test the polygon.reverse() function
    public void testPolygonReverse() {
        VLPolygon poly = poly(12.3402039, 45.4342526, 12.3401691, 45.4343433, 12.3401433, 45.4343973,
                12.3402433, 45.4344174, 12.3402845, 45.4344296, 12.3404923, 45.4338996, 12.3401159,
                45.4338161, 12.339956, 45.43421);
        VLPolygon poly2 = poly(12.3402039, 45.4342526, 12.339956, 45.43421, 12.3401159, 45.4338161,
                12.3404923, 45.4338996, 12.3402845, 45.4344296, 12.3402433, 45.4344174, 12.3401433,
                45.4343973, 12.3401691, 45.4343433);
        poly.reverse();
        assertTrue(poly.equals(poly2));
    }

    // Another Venice test case
    public void testPalazzo() {
        VLPolygon poly = poly(12.3402039, 45.4342526, 12.339956, 45.43421, 12.3401159, 45.4338161,
                12.3404923, 45.4338996, 12.3402845, 45.4344296, 12.3402433, 45.4344174, 12.3401433,
                45.4343973, 12.3401691, 45.4343433);

        Environment environment = new Environment(Arrays.asList(poly));
        environment.enforce_standard_form();
        VisibilityGraph vg = new VisibilityGraph(environment, 0.0000001);

        boolean expected[][] = {{ true,  true,  true, false, false, false, false,  true},
                                { true,  true,  true,  true,  true, false, false,  true},
                                { true,  true,  true,  true,  true,  true,  true,  true},
                                {false,  true,  true,  true,  true, false,  true,  true},
                                {false,  true,  true,  true,  true,  true,  true,  true},
                                {false, false,  true, false,  true,  true,  true, false},
                                {false, false,  true,  true,  true,  true,  true,  true},
                                { true,  true,  true,  true,  true, false,  true,  true}};

        for (int i = 0; i < expected.length; ++i) {
            for (int j = 0; j < expected[i].length; ++j) {
                assertEquals(" at " + i + ", " + j, expected[i][j], vg.get(i, j));
            }
        }
    }

    // The Piazza San Marco in Venice, from OSM.
    public void testSanMarco() {
        VLPolygon poly = poly(12.3389861, 45.4339415, 12.3389153, 45.4340763, 12.3390769, 45.4341172,
                12.3391694, 45.4341388, 12.3392136, 45.4340533, 12.3397036, 45.434177, 12.339797,
                45.4341939, 12.3397873, 45.4342286, 12.339706, 45.4342158, 12.3396866, 45.4342575,
                12.3393905, 45.434195, 12.3391779, 45.4346848, 12.3391272, 45.4347845, 12.3390937,
                45.4347676, 12.3389625, 45.4347071, 12.3386095, 45.4345509, 12.3379792, 45.4342771,
                12.3378901, 45.4342367, 12.3376881, 45.4341478, 12.337471, 45.4340513, 12.3373322,
                45.4339869, 12.3371759, 45.4339216, 12.3372951, 45.4336885, 12.3374061, 45.4334649,
                12.3374185, 45.4334486, 12.3391652, 45.4339348, 12.3391453, 45.4339861);

        Environment environment = new Environment(Arrays.asList(poly));
        environment.enforce_standard_form();

        VisibilityGraph vg = new VisibilityGraph(environment, 0.0000001);

        boolean expected[][] = {
                { true,  true, false,  true,  true, false,  true,  true,  true,  true, false, false, false, false,  true, false,  true,  true, false, false, false, false, false, false, false, false,  true},
                { true,  true,  true,  true,  true, false,  true,  true,  true, false, false, false, false, false, false, false,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},
                {false,  true,  true,  true,  true,  true,  true,  true, false, false, false, false, false, false, false, false, false,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},
                { true,  true,  true,  true,  true,  true,  true,  true, false, false, false, false, false, false, false, false, false,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},
                { true,  true,  true,  true,  true,  true,  true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,  true},
                {false, false,  true,  true,  true,  true,  true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false},
                { true,  true,  true,  true,  true,  true,  true,  true, false, false, false, false, false, false, false, false, false, false, false, false, false,  true,  true,  true,  true,  true,  true},
                { true,  true,  true,  true, false, false,  true,  true,  true, false, false, false, false, false, false, false, false,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},
                { true,  true, false, false, false, false, false,  true,  true,  true, false, false, false, false, false, false,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},
                { true, false, false, false, false, false, false, false,  true,  true,  true,  true,  true, false,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},
                {false, false, false, false, false, false, false, false, false,  true,  true,  true, false,  true,  true,  true,  true,  true,  true,  true,  true, false, false, false, false, false, false},
                {false, false, false, false, false, false, false, false, false,  true,  true,  true,  true,  true,  true,  true,  true, false, false, false, false, false, false,  true,  true,  true, false},
                {false, false, false, false, false, false, false, false, false,  true, false,  true,  true,  true,  true, false,  true, false, false, false, false, false, false, false,  true,  true,  true},
                {false, false, false, false, false, false, false, false, false, false,  true,  true,  true,  true,  true, false, false, false, false, false, false, false, false, false, false, false, false},
                { true, false, false, false, false, false, false, false, false,  true,  true,  true,  true,  true,  true,  true,  true, false, false, false, false, false, false, false, false,  true,  true},
                {false, false, false, false, false, false, false, false, false,  true,  true,  true, false, false,  true,  true,  true, false, false, false, false, false, false, false, false, false, false},
                { true,  true, false, false, false, false, false, false,  true,  true,  true,  true,  true, false,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},
                { true,  true,  true,  true, false, false, false,  true,  true,  true,  true, false, false, false, false, false,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true},
                {false,  true,  true,  true, false, false, false,  true,  true,  true,  true, false, false, false, false, false,  true,  true,  true,  true, false, false, false, false, false, false, false},
                {false,  true,  true,  true, false, false, false,  true,  true,  true,  true, false, false, false, false, false,  true,  true,  true,  true,  true, false, false, false, false, false, false},
                {false,  true,  true,  true, false, false, false,  true,  true,  true,  true, false, false, false, false, false,  true,  true, false,  true,  true,  true, false, false, false, false, false},
                {false,  true,  true,  true, false, false,  true,  true,  true,  true, false, false, false, false, false, false,  true,  true, false, false,  true,  true,  true,  true,  true,  true,  true},
                {false,  true,  true,  true, false, false,  true,  true,  true,  true, false, false, false, false, false, false,  true,  true, false, false, false,  true,  true,  true, false, false, false},
                {false,  true,  true,  true, false, false,  true,  true,  true,  true, false,  true, false, false, false, false,  true,  true, false, false, false,  true,  true,  true,  true,  true,  true},
                {false,  true,  true,  true, false, false,  true,  true,  true,  true, false,  true,  true, false, false, false,  true,  true, false, false, false,  true, false,  true,  true,  true,  true},
                {false,  true,  true,  true, false, false,  true,  true,  true,  true, false,  true,  true, false,  true, false,  true,  true, false, false, false,  true, false,  true,  true,  true,  true},
                { true,  true,  true,  true,  true, false,  true,  true,  true,  true, false, false,  true, false,  true, false,  true,  true, false, false, false,  true, false,  true,  true,  true,  true}
        };

        for (int i = 0; i < expected.length; ++i) {
            for (int j = 0; j < expected[i].length; ++j) {
                assertEquals(" at " + i + ", " + j, expected[i][j], vg.get(i, j));
            }
        }
    }

    // A massively reduced version of the Piazza San Marco, which
    // caused problems when the code was buggy
    public void testConcavePolygon2() {
        VLPolygon poly = poly(17.0, 14.0, 70.0, 18.0, 69.0, 26.0, 39.0, 20.0, 13.0, 78.0, -111.0,
                24.0

        );

        Environment environment = new Environment(Arrays.asList(poly));
        environment.enforce_standard_form();

        VisibilityGraph vg = new VisibilityGraph(environment, 0.0000001);

        boolean expected[][] = { { true, true, true, false, true, true },
                { true, true, true, true, true, true }, { true, true, true, true, true, false },
                { false, true, true, true, true, false }, { true, true, true, true, true, true },
                { true, true, false, false, true, true } };

        for (int i = 0; i < expected.length; ++i) {
            for (int j = 0; j < expected[i].length; ++j) {
                assertEquals(" at " + i + ", " + j, expected[i][j], vg.get(i, j));
            }
        }
    }

    public void testConcavePolygon() {
        VLPolygon poly = poly(1, 1, 5, 1, 5, 5, 3, 5, 3, 4, 4, 4, 4, 2, 2, 2, 2, 3, 1, 3);

        if (poly.area() < 0) {
            poly.reverse();
        }
        Environment environment = new Environment(Arrays.asList(poly));
        environment.enforce_standard_form();

        VisibilityGraph vg = new VisibilityGraph(environment, 0.01);
        boolean expected[][] = {
                { true, true, false, false, false, false, true, true, true, true },
                { true, true, true, false, false, true, true, true, false, false },
                { false, true, true, true, true, true, true, false, false, false },
                { false, false, true, true, true, true, false, false, false, false },
                { false, false, true, true, true, true, false, false, false, false },
                { false, true, true, true, true, true, true, false, false, false },
                { true, true, true, false, false, true, true, true, false, false },
                { true, true, false, false, false, false, true, true, true, true },
                { true, false, false, false, false, false, false, true, true, true },
                { true, false, false, false, false, false, false, true, true, true } };

        for (int i = 0; i < expected.length; ++i) {
            for (int j = 0; j < expected[i].length; ++j) {
                assertEquals(expected[i][j], vg.get(i, j));
            }
        }
    }

    public void testSquareCenteredInSquare() {

        Environment environment = new Environment(Arrays.asList(poly(1, 1, 5, 1, 5, 5, 1, 5),
                poly(2, 3.5, 2, 4.5, 3, 4.5, 3, 3.5)));
        environment.enforce_standard_form();

        VisibilityGraph vg = new VisibilityGraph(environment, 0.01);

        boolean expected[][] = { { true, true, true, true, true, true, false, true },
                { true, true, true, false, true, false, true, true },
                { true, true, true, true, false, true, true, true },
                { true, false, true, true, true, true, true, false },
                { true, true, false, true, true, true, false, true },
                { true, false, true, true, true, true, true, false },
                { false, true, true, true, false, true, true, true },
                { true, true, true, false, true, false, true, true } };

        for (int i = 0; i < expected.length; ++i) {
            for (int j = 0; j < expected[i].length; ++j) {
                assertEquals("difference at " + i + ", " + j, expected[i][j], vg.get(i, j));
            }
        }

    }

    public void testSquareOffCenterInSquare() {
        Environment environment = new Environment(Arrays.asList(poly(1, 1, 5, 1, 5, 5, 1, 5),
                poly(2, 2, 2, 3, 3, 3, 3, 2)));
        environment.enforce_standard_form();

        VisibilityGraph vg = new VisibilityGraph(environment, 0.01);

        boolean expected[][] = { { true, true, false, true, true, true, false, true },
                { true, true, true, true, true, false, true, true },
                { false, true, true, true, false, true, true, true },
                { true, true, true, true, true, true, true, false },
                { true, true, false, true, true, true, false, true },
                { true, false, true, true, true, true, true, false },
                { false, true, true, true, false, true, true, true },
                { true, true, true, false, true, false, true, true } };
        for (int i = 0; i < expected.length; ++i) {
            for (int j = 0; j < expected[i].length; ++j) {
                assertEquals(expected[i][j], vg.get(i, j));
            }
        }

    }

    public static VLPolygon poly(double... coords) {
        ArrayList<VLPoint> points = new ArrayList<VLPoint>();
        for (int i = 0; i < coords.length; i += 2) {
            VLPoint point = new VLPoint(coords[i], coords[i + 1]);
            points.add(point);
        }
        return new VLPolygon(points);
    }

}