package org.opentripplanner.graph_builder.module.stopsAlerts;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.StopVertex;

public interface IStopTester {
    boolean fulfillDemands(StopVertex ts, Graph graph);
    String getType();
}
