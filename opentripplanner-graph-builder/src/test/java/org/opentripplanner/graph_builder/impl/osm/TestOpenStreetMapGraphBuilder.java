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
import org.opentripplanner.routing.edgetype.TurnEdge;


public class TestOpenStreetMapGraphBuilder extends TestCase {

    @Test
    public void testGraphBuilder() throws Exception {

        Graph gg = new Graph();

        OpenStreetMapGraphBuilderImpl loader = new OpenStreetMapGraphBuilderImpl();
        FileBasedOpenStreetMapProviderImpl provider = new FileBasedOpenStreetMapProviderImpl();

        File file = new File(getClass().getResource("map.osm.gz").getFile());

        provider.setPath(file);
        loader.setProvider(provider);

        loader.buildGraph(gg);

        Vertex v2 = gg.getVertex("way 25660216 from 1"); //Kamiennogorska
        Vertex v2back = gg.getVertex("way 25660216 from 1 back"); //Kamiennogorska back
        Vertex v3 = gg.getVertex("way 25691274 from 0"); //Mariana Smoluchowskiego, right from Kamiennogorska
        Vertex v3back = gg.getVertex("way 25691274 from 0 back"); //ditto back
        Vertex v4 = gg.getVertex("way 25691274 from 3"); //Mariana Smoluchowskiego, left from Kamiennogorska
        Vertex v4back = gg.getVertex("way 25691274 from 3 back"); //ditto back
        assertNotNull(v2);
        assertNotNull(v2back);
        assertNotNull(v3);
        assertNotNull(v3back);
        assertNotNull(v4);
        assertNotNull(v4back);
        
        assertTrue ("name of v2 must be like \"Kamiennog\u00F3rska\"; was " + v2.getName(), v2.getName().contains("Kamiennog\u00F3rska"));
        assertTrue ("name of v3 must be like \"Mariana Smoluchowskiego\"; was " + v3.getName(), v3.getName().contains("Mariana Smoluchowskiego"));
        
        boolean v3EdgeExists = false;
        boolean v4EdgeExists = false;
        boolean v4BackEdgeExists = false;
        for (Edge e : gg.getOutgoing(v2)) {
            if (e instanceof TurnEdge) {
                TurnEdge t = (TurnEdge) e;
                Vertex tov = t.getToVertex();
                if (tov == v3 || tov == v3back) {
                    assertTrue("Turn cost wrong; expected ~90, was: " + t.turnCost, Math.abs(t.turnCost - 90) < 3);
                    v3EdgeExists = true;
                }
            }
        }
        
        for (Edge e : gg.getOutgoing(v2back)) {
            if (e instanceof TurnEdge) {
                TurnEdge t = (TurnEdge) e;
                Vertex tov = t.getToVertex();
                if (tov == v3 || tov == v3back) {
                    assertTrue(Math.abs(t.turnCost - 90) < 5);
                    v3EdgeExists = true;
                }
            }
        }
        
        for (Edge e : gg.getOutgoing(v3)) {
            if (e instanceof TurnEdge) {
                TurnEdge t = (TurnEdge) e;
                Vertex tov = t.getToVertex();
                if (tov == v4) {
                    assertTrue("Turn cost too big: " + t.turnCost, t.turnCost < 5);
                    v4EdgeExists = true;
                }
            }
        }
        
        for (Edge e : gg.getOutgoing(v4back)) {
            if (e instanceof TurnEdge) {
                TurnEdge t = (TurnEdge) e;
                Vertex tov = t.getToVertex();
                if (tov == v3back) {
                    assertTrue("Turn cost too big: " + t.turnCost, t.turnCost < 5);
                    v4BackEdgeExists = true;
                }
            }
        }

        assertTrue("There is no edge from v2 to v3", v3EdgeExists);
        assertTrue("There is no edge from v3 to v4", v4EdgeExists);
        assertTrue("There is no edge from v4back to v3back", v4BackEdgeExists);
    }
}
