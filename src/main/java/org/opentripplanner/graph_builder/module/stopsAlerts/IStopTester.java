package org.opentripplanner.graph_builder.module.stopsAlerts;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

public interface IStopTester {
    boolean fulfillDemands(TransitStopVertex ts, Graph graph);
    String getType();
}
