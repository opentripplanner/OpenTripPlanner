package org.opentripplanner.routing.vertextype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.module.osm.OSMFilter;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;

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

        /* test that traversal limitations work also without barrier tag  */
        OSMNode accessBarrier = new OSMNode();
        accessBarrier.addTag("access", "no");

        bv.setBarrierPermissions(OSMFilter
            .getPermissionsForEntity(accessBarrier, BarrierVertex.defaultBarrierPermissions));
        assertEquals(StreetTraversalPermission.NONE, bv.getBarrierPermissions());
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

        assertTrue(bv_to_endVertex_forward.canTraverse(new TraverseModeSet(TraverseMode.CAR)));
        assertTrue(bv_to_endVertex_forward.canTraverse(new TraverseModeSet(TraverseMode.BICYCLE)));
        assertTrue(bv_to_endVertex_forward.canTraverse(new TraverseModeSet(TraverseMode.WALK)));

        assertFalse(bv_to_endVertex_forward.canTraverseIncludingBarrier(TraverseMode.CAR));
        assertTrue(bv_to_endVertex_forward.canTraverseIncludingBarrier(TraverseMode.BICYCLE));
        assertTrue(bv_to_endVertex_forward.canTraverseIncludingBarrier(TraverseMode.WALK));

        StreetEdge endVertex_to_bv_backward = edge(endVertex, bv, 100, true);

        assertTrue(endVertex_to_bv_backward.canTraverse(new TraverseModeSet(TraverseMode.CAR)));
        assertTrue(endVertex_to_bv_backward.canTraverse(new TraverseModeSet(TraverseMode.BICYCLE)));
        assertTrue(endVertex_to_bv_backward.canTraverse(new TraverseModeSet(TraverseMode.WALK)));

        assertFalse(endVertex_to_bv_backward.canTraverseIncludingBarrier(TraverseMode.CAR));
        assertTrue(endVertex_to_bv_backward.canTraverseIncludingBarrier(TraverseMode.BICYCLE));
        assertTrue(endVertex_to_bv_backward.canTraverseIncludingBarrier(TraverseMode.WALK));

        StreetEdge bv_to_endVertex_backward = edge(bv, endVertex, 100, true);

        assertTrue(bv_to_endVertex_backward.canTraverse(new TraverseModeSet(TraverseMode.CAR)));
        assertTrue(bv_to_endVertex_backward.canTraverse(new TraverseModeSet(TraverseMode.BICYCLE)));
        assertTrue(bv_to_endVertex_backward.canTraverse(new TraverseModeSet(TraverseMode.WALK)));

        assertFalse(bv_to_endVertex_backward.canTraverseIncludingBarrier(TraverseMode.CAR));
        assertTrue(bv_to_endVertex_backward.canTraverseIncludingBarrier(TraverseMode.BICYCLE));
        assertTrue(bv_to_endVertex_backward.canTraverseIncludingBarrier(TraverseMode.WALK));

        StreetEdge endVertex_to_bv_forward = edge(endVertex, bv, 100, false);

        assertTrue(endVertex_to_bv_forward.canTraverse(new TraverseModeSet(TraverseMode.CAR)));
        assertTrue(endVertex_to_bv_forward.canTraverse(new TraverseModeSet(TraverseMode.BICYCLE)));
        assertTrue(endVertex_to_bv_forward.canTraverse(new TraverseModeSet(TraverseMode.WALK)));

        assertFalse(endVertex_to_bv_forward.canTraverseIncludingBarrier(TraverseMode.CAR));
        assertTrue(endVertex_to_bv_forward.canTraverseIncludingBarrier(TraverseMode.BICYCLE));
        assertTrue(endVertex_to_bv_forward.canTraverseIncludingBarrier(TraverseMode.WALK));


        //tests bollard which doesn't allow cycling
        BarrierVertex noBicycleBollard = new BarrierVertex(_graph, "no_bike_bollard", 1.5, 1, 0);
        noBicycleBollard.setBarrierPermissions(StreetTraversalPermission.PEDESTRIAN);
        StreetEdge no_bike_to_endVertex = edge(noBicycleBollard, endVertex, 100, false);

        assertTrue(no_bike_to_endVertex.canTraverse(new TraverseModeSet(TraverseMode.CAR)));
        assertTrue(no_bike_to_endVertex.canTraverse(new TraverseModeSet(TraverseMode.BICYCLE)));
        assertTrue(no_bike_to_endVertex.canTraverse(new TraverseModeSet(TraverseMode.WALK)));

        assertFalse(no_bike_to_endVertex.canTraverseIncludingBarrier(TraverseMode.CAR));
        assertFalse(no_bike_to_endVertex.canTraverseIncludingBarrier(TraverseMode.BICYCLE));
        assertTrue(no_bike_to_endVertex.canTraverseIncludingBarrier(TraverseMode.WALK));
    }
}