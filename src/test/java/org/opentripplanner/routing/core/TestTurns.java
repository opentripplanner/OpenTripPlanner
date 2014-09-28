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

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.GeometryFactory;

import junit.framework.TestCase;

public class TestTurns extends TestCase {

    public void testIntersectionVertex() {

        GeometryFactory gf = new GeometryFactory();

        LineString geometry = gf.createLineString(new Coordinate[] {
                new Coordinate(-0.10, 0),
                new Coordinate(0, 0)
                });

        IntersectionVertex v1 = new IntersectionVertex(null, "v1", -0.10, 0);
        IntersectionVertex v2 = new IntersectionVertex(null, "v2", 0, 0);
        
        StreetEdge leftEdge = new StreetEdge(v1, v2, geometry, "morx", 10.0, StreetTraversalPermission.ALL, true);

        LineString geometry2 = gf.createLineString(new Coordinate[] {
                new Coordinate(0, 0),
                new Coordinate(-0.10, 0)
                });

        StreetEdge rightEdge = new StreetEdge(v1, v2, geometry2, "fleem", 10.0, StreetTraversalPermission.ALL, false);

        assertEquals(180, Math.abs(leftEdge.getOutAngle() - rightEdge.getOutAngle()));

    }

}
