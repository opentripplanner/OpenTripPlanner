package org.opentripplanner.graph_builder.module.stopsAlerts;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;

public interface IStopTester {
    boolean fulfillDemands(TransitStop ts, Graph graph);
    String getType();
}
