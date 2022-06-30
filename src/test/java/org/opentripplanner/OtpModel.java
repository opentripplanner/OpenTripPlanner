package org.opentripplanner;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.TransitModel;

public class OtpModel {

  public final Graph graph;
  public final TransitModel transitModel;

  public OtpModel(Graph graph, TransitModel transitModel) {
    this.graph = graph;
    this.transitModel = transitModel;
  }
}
