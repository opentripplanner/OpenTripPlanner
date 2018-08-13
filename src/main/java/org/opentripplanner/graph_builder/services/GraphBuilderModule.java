package org.opentripplanner.graph_builder.services;

import java.util.HashMap;
import java.util.List;

import org.opentripplanner.routing.graph.Graph;

/** Modules that add elements to a graph. These are plugins to the GraphBuilder. */
public interface GraphBuilderModule {

    /** Process whatever inputs were supplied to this module and add the resulting elements to the given graph. */
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra);

    /** Check that all inputs to the graphbuilder are valid; throw an exception if not. */
    public void checkInputs();

}
