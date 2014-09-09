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

import junit.framework.TestCase;

import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class CompactLineStringTest extends TestCase {

    @Test
    public final void testCompactString() {

        GeometryFactory gf = new GeometryFactory();

        List<Coordinate> c = new ArrayList<Coordinate>();
        double x0 = 1.111111111;
        double y0 = 0.123456789;
        double x1 = 2.0;
        double y1 = 0.0;
        c.add(new Coordinate(x0, y0));
        c.add(new Coordinate(x1, y1));
        LineString ls = gf.createLineString(c.toArray(new Coordinate[0]));
        CompactLineString cls = CompactLineString.create(x0, y0, x1, y1, ls);
        assertTrue(cls == CompactLineString.STRAIGHT_LINE); // ==, not equals
        LineString ls2 = cls.toLineString(x0, y0, x1, y1);
        assertTrue(ls.equalsExact(ls2, 0.00000015));

        c.clear();
        c.add(new Coordinate(x0, y0));
        c.add(new Coordinate(-179.99, 1.12345));
        c.add(new Coordinate(179.99, 1.12345));
        c.add(new Coordinate(x1, y1));
        ls = gf.createLineString(c.toArray(new Coordinate[0]));
        cls = CompactLineString.create(x0, y0, x1, y1, ls);
        assertTrue(cls != CompactLineString.STRAIGHT_LINE);
        ls2 = cls.toLineString(x0, y0, x1, y1);
        assertTrue(ls.equalsExact(ls2, 0.00000015));
    }
}
