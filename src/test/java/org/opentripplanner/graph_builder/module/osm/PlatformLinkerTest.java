package org.opentripplanner.graph_builder.module.osm;


import org.junit.Test;
import org.opentripplanner.graph_builder.module.FakeGraph;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import java.io.File;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


public class PlatformLinkerTest {

    @Test
    public void testRayCasting(){

        final double[][] platform = {{10.5242855, 59.8925348}, {10.524743, 59.8927012}, { 10.5248089, 59.8926397}, {10.5243485, 59.8924888}};

        double[] testPointInside = { 10.524729, 59.8926474};
        double[] testPointOutside = { 10.5249007, 59.8925344};

        assert(PlatformLinker.contains(platform, testPointInside));
        assert(!PlatformLinker.contains(platform, testPointOutside));
    }

    @Test
    public void testRayCasting_Hellerud(){
        final double[][] platform = {{ 10.830791300000001, 59.910225700000005},
                { 10.830805, 59.910188700000006},
                { 10.8288492, 59.910006100000004},
                { 10.828837700000001, 59.910037},
                { 10.8297421, 59.9101177},
                { 10.8297992, 59.91012980000001},
                { 10.8300367, 59.910152700000005},
                { 10.8300263, 59.910181},
                { 10.830367, 59.910212800000004},
                { 10.8303726, 59.910197600000004},
                { 10.830376600000001, 59.91018690000001}
        };

        double[] testPointOutside = { 10.6923612, 59.910212800000005};
        assert(!PlatformLinker.contains(platform, testPointOutside));
    }

    /**
     * Test linking from stairs endpoint to to nodes in the ring defining the platform area.
     * OSM test data is from Skøyen station, Norway
     */
    @Test
    public void testLinkEntriesToPlatforms() throws Exception {

        String stairsEndpointLabel = "osm:node:1028861028";

        List<String> platformRingVertexLabels = Arrays.asList("osm:node:304045332", "osm:node:3238357455", "osm:node:1475363433",
                "osm:node:3238357491", "osm:node:1475363427", "osm:node:304045336", "osm:node:304045337", "osm:node:1475363437",
                "osm:node:3238357483", "osm:node:1475363443", "osm:node:1028860941", "osm:node:304045341", "osm:node:304045332");


        Graph gg = new Graph();
        OpenStreetMapModule loader = new OpenStreetMapModule();
        loader.platformEntriesLinking = true;
        loader.skipVisibility = true;
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        AnyFileBasedOpenStreetMapProviderImpl provider = new AnyFileBasedOpenStreetMapProviderImpl();

        File file = new File(
                URLDecoder.decode(FakeGraph.class.getResource("osm/skoyen.osm.pbf").getFile(),
                        "UTF-8"));

        provider.setPath(file);
        loader.setProvider(provider);
        loader.buildGraph(gg, new HashMap<>());

        Vertex stairsEndpoint = gg.getVertex(stairsEndpointLabel);

        // verify outgoing links
        List<String> linkedRingVertecies = stairsEndpoint.getOutgoing().stream().map(edge -> edge.getToVertex().getLabel()).collect(Collectors.toList());
        assertEquals(linkedRingVertecies.size() -2, platformRingVertexLabels.size());  // the endpoint has links to two nodes in OSM
        for(String label : platformRingVertexLabels){
            assert(linkedRingVertecies.contains(label));
        }

        // verify incoming links
        List<String> linkedRingVerteciesInn = stairsEndpoint.getIncoming().stream().map(edge -> edge.getFromVertex().getLabel()).collect(Collectors.toList());
        assertEquals(linkedRingVerteciesInn.size() -2, platformRingVertexLabels.size());  // the endpoint has links to two nodes in OSM
        for(String label : platformRingVertexLabels){
            assert(linkedRingVerteciesInn.contains(label));
        }

    }

}
