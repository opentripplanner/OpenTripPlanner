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

package org.opentripplanner.routing.vertextype;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.junit.Test;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.module.osm.OSMFilter;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;

import static org.junit.Assert.*;

/**
 * Created by mabu on 17.8.2015.
 */
public class BarrierVertexTest {

    @Test
    public void testBarrierPermissions() throws Exception {
        OSMNode simpleBarier = new OSMNode();
        assertFalse(simpleBarier.isBollard());
        simpleBarier.addTag("barrier", "bollard");
        assertTrue(simpleBarier.isBollard());
        Graph graph = new Graph();
        String label = "simpleBarrier";
        BarrierVertex bv = new BarrierVertex(graph, label, simpleBarier.lon, simpleBarier.lat, 0);
        bv.setBarrierPermissions(OSMFilter
            .getPermissionsForEntity(simpleBarier, BarrierVertex.defaultBarrierPermissions));
        assertEquals(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, bv.getBarrierPermissions());

        simpleBarier.addTag("foot", "yes");
        bv.setBarrierPermissions(OSMFilter
            .getPermissionsForEntity(simpleBarier, BarrierVertex.defaultBarrierPermissions));
        assertEquals(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, bv.getBarrierPermissions());
        simpleBarier.addTag("bicycle", "yes");
        bv.setBarrierPermissions(OSMFilter
            .getPermissionsForEntity(simpleBarier, BarrierVertex.defaultBarrierPermissions));
        assertEquals(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, bv.getBarrierPermissions());
        simpleBarier.addTag("access", "no");
        bv.setBarrierPermissions(OSMFilter
            .getPermissionsForEntity(simpleBarier, BarrierVertex.defaultBarrierPermissions));
        assertEquals(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, bv.getBarrierPermissions());

        simpleBarier.addTag("motor_vehicle", "no");
        bv.setBarrierPermissions(OSMFilter
            .getPermissionsForEntity(simpleBarier, BarrierVertex.defaultBarrierPermissions));
        assertEquals(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE, bv.getBarrierPermissions());

        simpleBarier.addTag("bicycle", "no");
        bv.setBarrierPermissions(OSMFilter
            .getPermissionsForEntity(simpleBarier, BarrierVertex.defaultBarrierPermissions));
        assertEquals(StreetTraversalPermission.PEDESTRIAN, bv.getBarrierPermissions());

        OSMNode complexBarrier = new OSMNode();
        complexBarrier.addTag("barrier", "bollard");
        complexBarrier.addTag("access", "no");

        bv.setBarrierPermissions(OSMFilter
            .getPermissionsForEntity(complexBarrier, BarrierVertex.defaultBarrierPermissions));
        assertEquals(StreetTraversalPermission.NONE, bv.getBarrierPermissions());

        OSMNode noBikeBollard = new OSMNode();
        noBikeBollard.addTag("barrier", "bollard");
        noBikeBollard.addTag("bicycle", "no");

        bv.setBarrierPermissions(OSMFilter
            .getPermissionsForEntity(noBikeBollard, BarrierVertex.defaultBarrierPermissions));
        assertEquals(StreetTraversalPermission.PEDESTRIAN, bv.getBarrierPermissions());
    }

    /**
     * Create an edge. If twoWay, create two edges (back and forth).
     *
     * @param vA
     * @param vB
     * @param length
     * @param back true if this is a reverse edge
     */
    private StreetEdge edge(StreetVertex vA, StreetVertex vB, double length, boolean back) {
        String labelA = vA.getLabel();
        String labelB = vB.getLabel();
        String name = String.format("%s_%s", labelA, labelB);
        Coordinate[] coords = new Coordinate[2];
        coords[0] = vA.getCoordinate();
        coords[1] = vB.getCoordinate();
        LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

        StreetTraversalPermission perm = StreetTraversalPermission.ALL;
        return new StreetEdge(vA, vB, geom, name, length, perm, back);
    }



    @Test
    public void testStreetsWithBollard() {
        Graph _graph = new Graph();
        //default permissions are PEDESTRIAND and BICYCLE
        BarrierVertex bv = new BarrierVertex(_graph, "start_bollard", 2.0, 2.0, 0);

        StreetVertex endVertex = new IntersectionVertex(_graph, "end_vertex", 1.0, 2.0);

        StreetEdge bv_to_endVertex_forward = edge(bv, endVertex, 100, false);

        assertTrue(bv_to_endVertex_forward.canTraverse(new TraverseModeSet("CAR")));
        assertTrue(bv_to_endVertex_forward.canTraverse(new TraverseModeSet("BICYCLE")));
        assertTrue(bv_to_endVertex_forward.canTraverse(new TraverseModeSet("WALK")));

        assertFalse(bv_to_endVertex_forward.canTraverseIncludingBarrier(TraverseMode.CAR));
        assertTrue(bv_to_endVertex_forward.canTraverseIncludingBarrier(TraverseMode.BICYCLE));
        assertTrue(bv_to_endVertex_forward.canTraverseIncludingBarrier(TraverseMode.WALK));

        StreetEdge endVertex_to_bv_backward = edge(endVertex, bv, 100, true);

        assertTrue(endVertex_to_bv_backward.canTraverse(new TraverseModeSet("CAR")));
        assertTrue(endVertex_to_bv_backward.canTraverse(new TraverseModeSet("BICYCLE")));
        assertTrue(endVertex_to_bv_backward.canTraverse(new TraverseModeSet("WALK")));

        assertFalse(endVertex_to_bv_backward.canTraverseIncludingBarrier(TraverseMode.CAR));
        assertTrue(endVertex_to_bv_backward.canTraverseIncludingBarrier(TraverseMode.BICYCLE));
        assertTrue(endVertex_to_bv_backward.canTraverseIncludingBarrier(TraverseMode.WALK));

        StreetEdge bv_to_endVertex_backward = edge(bv, endVertex, 100, true);

        assertTrue(bv_to_endVertex_backward.canTraverse(new TraverseModeSet("CAR")));
        assertTrue(bv_to_endVertex_backward.canTraverse(new TraverseModeSet("BICYCLE")));
        assertTrue(bv_to_endVertex_backward.canTraverse(new TraverseModeSet("WALK")));

        assertFalse(bv_to_endVertex_backward.canTraverseIncludingBarrier(TraverseMode.CAR));
        assertTrue(bv_to_endVertex_backward.canTraverseIncludingBarrier(TraverseMode.BICYCLE));
        assertTrue(bv_to_endVertex_backward.canTraverseIncludingBarrier(TraverseMode.WALK));

        StreetEdge endVertex_to_bv_forward = edge(endVertex, bv, 100, false);

        assertTrue(endVertex_to_bv_forward.canTraverse(new TraverseModeSet("CAR")));
        assertTrue(endVertex_to_bv_forward.canTraverse(new TraverseModeSet("BICYCLE")));
        assertTrue(endVertex_to_bv_forward.canTraverse(new TraverseModeSet("WALK")));

        assertFalse(endVertex_to_bv_forward.canTraverseIncludingBarrier(TraverseMode.CAR));
        assertTrue(endVertex_to_bv_forward.canTraverseIncludingBarrier(TraverseMode.BICYCLE));
        assertTrue(endVertex_to_bv_forward.canTraverseIncludingBarrier(TraverseMode.WALK));


        //tests bollard which doesn't allow cycling
        BarrierVertex noBicycleBollard = new BarrierVertex(_graph, "no_bike_bollard", 1.5, 1, 0);
        noBicycleBollard.setBarrierPermissions(StreetTraversalPermission.PEDESTRIAN);
        StreetEdge no_bike_to_endVertex = edge(noBicycleBollard, endVertex, 100, false);

        assertTrue(no_bike_to_endVertex.canTraverse(new TraverseModeSet("CAR")));
        assertTrue(no_bike_to_endVertex.canTraverse(new TraverseModeSet("BICYCLE")));
        assertTrue(no_bike_to_endVertex.canTraverse(new TraverseModeSet("WALK")));

        assertFalse(no_bike_to_endVertex.canTraverseIncludingBarrier(TraverseMode.CAR));
        assertFalse(no_bike_to_endVertex.canTraverseIncludingBarrier(TraverseMode.BICYCLE));
        assertTrue(no_bike_to_endVertex.canTraverseIncludingBarrier(TraverseMode.WALK));
    }
}