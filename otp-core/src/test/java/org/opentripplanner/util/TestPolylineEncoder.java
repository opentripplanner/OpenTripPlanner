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

package org.opentripplanner.util;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.util.model.EncodedPolylineBean;

import com.vividsolutions.jts.geom.Coordinate;

import junit.framework.TestCase;

public class TestPolylineEncoder extends TestCase {

    public void testCreateEncodingsIterableOfCoordinate() {
        // test taken from example usage
        List<Coordinate> points = new ArrayList<Coordinate>();
        points.add(new Coordinate(-73.85062, 40.903125, Double.NaN));
        points.add(new Coordinate(-73.85136, 40.902261, Double.NaN));
        points.add(new Coordinate(-73.85151, 40.902066, Double.NaN));
        EncodedPolylineBean eplb = PolylineEncoder.createEncodings(points);
        assertEquals("o{sxFl}vaMjDpCf@\\", eplb.getPoints());
        assertEquals(3, eplb.getLength());
        assertNull(eplb.getLevels());
    }
}
