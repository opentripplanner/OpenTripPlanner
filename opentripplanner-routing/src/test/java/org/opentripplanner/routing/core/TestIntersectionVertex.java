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

package org.opentripplanner.routing.core;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import junit.framework.TestCase;

public class TestIntersectionVertex extends TestCase {

    public void testIntersectionVertex() {

        Intersection intersection = new Intersection("Morx at Fleem", 0, 0);
        GeometryFactory gf = new GeometryFactory();

        Geometry geometry = gf.createLineString(new Coordinate[] {
                new Coordinate(-10, 0),
                new Coordinate(0, 0)
                });

        IntersectionVertex leftV = new IntersectionVertex(intersection, geometry, true);

        Geometry geometry2 = gf.createLineString(new Coordinate[] {
                new Coordinate(0, 0),
                new Coordinate(10, 0)
                });

        IntersectionVertex rightV = new IntersectionVertex(intersection, geometry2, false);

        assertEquals(180, Math.abs(leftV.angle - rightV.angle));

    }

}
