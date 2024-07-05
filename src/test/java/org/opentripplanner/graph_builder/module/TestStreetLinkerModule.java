package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.TransitModel;

public class TestStreetLinkerModule {

  /** For test only */
  public static void link(Graph graph, TransitModel model) {
    new StreetLinkerModule(graph, model, DataImportIssueStore.NOOP, false).buildGraph();
  }
}
