package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.transit.service.TimetableRepository;

public class TestStreetLinkerModule {

  /** For test only */
  public static void link(Graph graph, TimetableRepository model) {
    new StreetLinkerModule(
      graph,
      new VehicleParkingService(),
      model,
      DataImportIssueStore.NOOP,
      false
    )
      .buildGraph();
  }
}
