package org.opentripplanner.graph_builder.services;

import org.opentripplanner.graph_builder.module.GraphBuilderModuleSummary;
import org.opentripplanner.routing.graph.Graph;

/** Modules that add elements to a graph. These are plugins to the GraphBuilder. */
public interface GraphBuilderModule {

    /**
     * Process whatever inputs were supplied to this module and add the resulting elements to the given graph.
     * Also, add build info to the given GraphBuilderModuleSummary as needed.
     */
    public void buildGraph(Graph graph, GraphBuilderModuleSummary graphBuilderModuleSummary);

    /** Check that all inputs to the graphbuilder are valid; throw an exception if not. */
    public void checkInputs();

}
