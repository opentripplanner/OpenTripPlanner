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
        assertTrue (v1.getName().contains("Kamiennogórska") && v1.getName().contains("Mariana Smoluchowskiego"));

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
