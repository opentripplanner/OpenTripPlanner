package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.transit.service.TimetableRepository;

public class TestStreetLinkerModule {

  /** For test only */
  public static void link(Graph graph, TimetableRepository timetableRepository) {
    link(graph, new DefaultVehicleParkingRepository(), timetableRepository);
  }

  public static void link(
    Graph graph,
    VehicleParkingRepository parkingRepository,
    TimetableRepository timetableRepository
  ) {
    new StreetLinkerModule(
      graph,
      parkingRepository,
      timetableRepository,
      DataImportIssueStore.NOOP,
      false
    ).buildGraph();
  }
}
