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

package org.opentripplanner.graph_builder.impl.osm;

import java.io.File;
import java.util.HashMap;

import junit.framework.TestCase;

import org.junit.Test;
import org.opentripplanner.openstreetmap.impl.FileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.edgetype.ParkAndRideLinkEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.ParkAndRideVertex;

public class TestUnconnectedAreas extends TestCase {

    /**
     * The P+R.osm.gz file contains 2 park and ride, one a single way area and the other a
     * multipolygon with a hole. Both are not linked to any street, apart from three roads that
     * crosses the P+R with w/o common nodes.
     * 
     * This test just make sure we correctly link those P+R with the street network by creating
     * virtual nodes at the place where the street intersects the P+R areas. See ticket #1562.
     */
    @Test
    public void testUnconnectedParkAndRide() throws Exception {

        Graph gg = new Graph();

        OpenStreetMapGraphBuilderImpl loader = new OpenStreetMapGraphBuilderImpl();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        FileBasedOpenStreetMapProviderImpl provider = new FileBasedOpenStreetMapProviderImpl();
        File file = new File(getClass().getResource("P+R.osm.gz").getFile());
        provider.setPath(file);
        loader.setProvider(provider);
        loader.buildGraph(gg, new HashMap<Class<?>, Object>());

        assertEquals(1, gg.getBuilderAnnotations().size());

        int nParkAndRide = 0;
        int nParkAndRideLink = 0;
        for (Vertex v : gg.getVertices()) {
            if (v instanceof ParkAndRideVertex) {
                nParkAndRide++;
            }
        }
        for (Edge e : gg.getEdges()) {
            if (e instanceof ParkAndRideLinkEdge) {
                nParkAndRideLink++;
            }
        }
        assertEquals(2, nParkAndRide);
        assertEquals(10, nParkAndRideLink);
    }
}
