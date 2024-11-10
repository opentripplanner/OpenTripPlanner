package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.transit.service.TimetableRepository;

public class TestStreetLinkerModule {

  /** For test only */
  public static void link(Graph graph, TimetableRepository timetableRepository) {
    link(graph, new VehicleParkingService(), timetableRepository);
  }

  public static void link(
    Graph graph,
    VehicleParkingService vehicleParkingService,
    TimetableRepository timetableRepository
  ) {
    new StreetLinkerModule(
      graph,
      vehicleParkingService,
      timetableRepository,
      DataImportIssueStore.NOOP,
      false
    )
      .buildGraph();
  }
}
