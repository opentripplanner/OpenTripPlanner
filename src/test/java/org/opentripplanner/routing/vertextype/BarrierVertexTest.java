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

import org.junit.Test;
import org.opentripplanner.graph_builder.module.osm.OSMFilter;
import org.opentripplanner.openstreetmap.model.OSMNode;
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
        BarrierVertex bv = new BarrierVertex(graph, label, simpleBarier.lon, simpleBarier.lat);
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
    }
}