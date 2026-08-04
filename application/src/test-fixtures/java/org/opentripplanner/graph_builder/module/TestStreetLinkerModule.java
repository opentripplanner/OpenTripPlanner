package org.opentripplanner.graph_builder.module;

import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.TransitRepository;

public class TestStreetLinkerModule {

  /** For test only */
  public static void link(Graph graph, TransitRepository transitRepository) {
    link(graph, new DefaultVehicleParkingRepository(), transitRepository);
  }

  public static void link(
    Graph graph,
    VehicleParkingRepository parkingRepository,
    TransitRepository transitRepository
  ) {
    new StreetLinkerModule(
      graph,
      VertexLinkerTestFactory.of(graph),
      parkingRepository,
      transitRepository,
      DataImportIssueStore.NOOP
    ).buildGraph();
  }
}
