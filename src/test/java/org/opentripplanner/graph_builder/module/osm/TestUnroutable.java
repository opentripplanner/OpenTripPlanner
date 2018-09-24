package org.opentripplanner.graph_builder.module.osm;


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
