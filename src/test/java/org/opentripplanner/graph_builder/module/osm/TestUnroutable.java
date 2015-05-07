package org.opentripplanner.graph_builder.module.osm;

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

import com.google.common.collect.Maps;
import junit.framework.TestCase;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;

/**
 * Verify that OSM ways that represent proposed or as yet unbuilt roads are not used for routing.
 * This tests functionality in or around the method isWayRoutable() in the OSM graph builder module.
 *
 * @author abyrd
 */
public class TestUnroutable extends TestCase {

    private Graph graph = new Graph();

    private AStar aStar = new AStar();

    public void setUp() throws Exception {
        OpenStreetMapModule osmBuilder = new OpenStreetMapModule();
        osmBuilder.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        AnyFileBasedOpenStreetMapProviderImpl provider = new AnyFileBasedOpenStreetMapProviderImpl();
        URL osmDataUrl = getClass().getResource("bridge_construction.osm.pbf");
        File osmDataFile = new File(URLDecoder.decode(osmDataUrl.getFile(), "UTF-8"));
        provider.setPath(osmDataFile);
        osmBuilder.setProvider(provider);
        HashMap<Class<?>, Object> extra = Maps.newHashMap();
        osmBuilder.buildGraph(graph, extra); // TODO get rid of this "extra" thing
     }

    /**
     * Search for a path across the Willamette river. This OSM data includes a bridge that is not yet built and is
     * therefore tagged highway=construction.
     * TODO also test unbuilt, proposed, raceways etc.
     */
    public void testOnBoardRouting() throws Exception {

        RoutingRequest options = new RoutingRequest();

        Vertex from = graph.getVertex("osm:node:2003617278");
        Vertex to = graph.getVertex("osm:node:40446276");
        options.setRoutingContext(graph, from, to);
        options.setMode(TraverseMode.BICYCLE);
        ShortestPathTree spt = aStar.getShortestPathTree(options);
        GraphPath path = spt.getPath(to, false);
        // At the time of writing this test, the router simply doesn't find a path at all when highway=construction
        // is filtered out, thus the null check.
        if (path != null) {
            for (Edge edge : path.edges) {
                assertFalse("Path should not use the as-yet unbuilt Tilikum Crossing bridge.",
                        "Tilikum Crossing".equals(edge.getName()));
            }
        }
    }
}
