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

import junit.framework.TestCase;

import org.junit.Test;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Turn;


public class OpenStreetMapGraphBuilderTest extends TestCase {

    @Test
    public void testGraphBuilder() throws Exception {

        Graph gg = new Graph();

        OpenStreetMapGraphBuilderImpl loader = new OpenStreetMapGraphBuilderImpl();
        FileBasedOpenStreetMapProviderImpl provider = new FileBasedOpenStreetMapProviderImpl();

        File file = new File(getClass().getResource("map.osm.gz").getFile());

        provider.setPath(file);
        loader.setProvider(provider);

        loader.buildGraph(gg);

        Vertex v1 = gg.getVertex("osm node 288969929 at 52");
        Vertex v2 = gg.getVertex("osm node 288969929 at 141");
        Vertex v3 = gg.getVertex("osm node 288969929 at 219");
        v1.getName();
        assertTrue ("name of v1 must be like \"Kamiennog\u00F3rska at Mariana Smoluchowskiego\"; was " + v1.getName(), v1.getName().contains("Kamiennog\u00F3rska") && v1.getName().contains("Mariana Smoluchowskiego"));

        for (Edge e : v1.getOutgoing()) {
            if (e instanceof Turn) {
                Turn t = (Turn) e;
                if (e.getToVertex() == v2) {
                    assertTrue(t.turnAngle > 80 && t.turnAngle < 100);
                    }
                if (e.getToVertex() == v3) {
                    assertTrue(t.turnAngle == 0);
                }
            }
        }
    }
}
