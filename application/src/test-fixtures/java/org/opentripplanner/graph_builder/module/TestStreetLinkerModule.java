package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.repository.TimetableBuildRepository;
import org.opentripplanner.transit.service.TransitRepository;

public class TestStreetLinkerModule {

  /** For test only */
  public static void link(
    Graph graph,
    TransitRepository transitRepository,
    TimetableBuildRepository timetableBuildRepository
  ) {
    link(graph, new DefaultVehicleParkingRepository(), transitRepository, timetableBuildRepository);
  }

  public static void link(
    Graph graph,
    VehicleParkingRepository parkingRepository,
    TransitRepository transitRepository,
    TimetableBuildRepository timetableBuildRepository
  ) {
    new StreetLinkerModule(
      graph,
      VertexLinkerTestFactory.of(graph),
      parkingRepository,
      transitRepository,
      timetableBuildRepository,
      DataImportIssueStore.NOOP
    ).buildGraph();
  }
}
