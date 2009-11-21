package org.opentripplanner.graph_builder.impl;

import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.edgetype.loader.NetworkLinker;

public class TransitToStreetNetworkGraphBuilderImpl implements GraphBuilder {

    @Override
    public void buildGraph(Graph graph) {
        NetworkLinker linker = new NetworkLinker(graph);
        linker.createLinkage();
    }

}
