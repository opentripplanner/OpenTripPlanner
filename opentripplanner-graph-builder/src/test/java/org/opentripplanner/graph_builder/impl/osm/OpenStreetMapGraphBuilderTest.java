package org.opentripplanner.graph_builder.impl.osm;

import java.io.File;

import org.junit.Test;
import org.opentripplanner.routing.core.Graph;

public class OpenStreetMapGraphBuilderTest {

    @Test
    public void testGraphBuilder() throws Exception {

        Graph gg = new Graph();

        OpenStreetMapGraphBuilderImpl loader = new OpenStreetMapGraphBuilderImpl();
        FileBasedOpenStreetMapProviderImpl provider = new FileBasedOpenStreetMapProviderImpl();

        File file = new File(getClass().getResource("map.osm.gz").getFile());

        provider.setPath(file);
        loader.setProvider(provider);

        loader.buildGraph(gg);

    }
}
