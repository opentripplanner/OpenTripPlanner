package org.opentripplanner;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.TransitModel;

public record TestOtpModel(Graph graph, TransitModel transitModel) {
  public TestOtpModel index() {
    transitModel.index();
    graph.index(transitModel.getStopModel());
    return this;
  }
}
