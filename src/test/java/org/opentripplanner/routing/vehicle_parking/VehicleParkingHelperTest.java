package org.opentripplanner.routing.vehicle_parking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.edgetype.VehicleParkingEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking.VehicleParkingEntranceCreator;
import org.opentripplanner.routing.vertextype.VehicleParkingEntranceVertex;
import org.opentripplanner.util.NonLocalizedString;

class VehicleParkingHelperTest {

  private static final String TEST_FEED_ID = "TEST";

  @Test
  void linkOnlyOneVertexTest() {
    Graph graph = new Graph();
    VehicleParking vehicleParking = createParingWithEntrances(1);

    VehicleParkingHelper.linkVehicleParkingToGraph(graph, vehicleParking);

    assertGraph(graph, 1);
  }

  @Test
  void linkThreeVerticesTest() {
    Graph graph = new Graph();
    VehicleParking vehicleParking = createParingWithEntrances(3);

    VehicleParkingHelper.linkVehicleParkingToGraph(graph, vehicleParking);

    assertGraph(graph, 3);
  }

  @Test
  void linkSkippingEdgesTest() {
    Graph graph = new Graph();
    var vehicleParking = VehicleParking
        .builder()
        .entrances(
                IntStream.rangeClosed(1, 3)
                        .<VehicleParkingEntranceCreator>mapToObj(id -> builder -> builder
                                .entranceId(new FeedScopedId(TEST_FEED_ID, "Entrance " + id))
                                .name(new NonLocalizedString("Entrance " + id))
                                .x(id)
                                .y(id)
                                .carAccessible(id == 1 || id == 3)
                                .walkAccessible(id == 2 || id == 3))
                .collect(Collectors.toList())
        )
        .wheelchairAccessibleCarPlaces(true)
        .build();

    VehicleParkingHelper.linkVehicleParkingToGraph(graph, vehicleParking);

    assertEquals(3, graph.getVerticesOfType(VehicleParkingEntranceVertex.class).size());
    assertEquals(7, graph.getEdgesOfType(VehicleParkingEdge.class).size());
  }

  private VehicleParking createParingWithEntrances(int entranceNumber) {
    return VehicleParking
        .builder()
        .bicyclePlaces(true)
        .entrances(
                IntStream.rangeClosed(1, entranceNumber)
                        .<VehicleParkingEntranceCreator>mapToObj(id -> builder -> builder
                                .entranceId(new FeedScopedId(TEST_FEED_ID, "Entrance " + id))
                                .name(new NonLocalizedString("Entrance " + id))
                                .x(id)
                                .y(id)
                                .walkAccessible(true))
                        .collect(Collectors.toList())
        )
        .build();
  }

  private void assertGraph(Graph graph, int vertexNumber) {
    assertEquals(vertexNumber, graph.getVertices().size());
    assertEquals(vertexNumber, graph.getVerticesOfType(VehicleParkingEntranceVertex.class).size());

    for (VehicleParkingEntranceVertex vehicleParkingEntranceVertex : graph.getVerticesOfType(
        VehicleParkingEntranceVertex.class)) {
      assertEquals(vertexNumber, vehicleParkingEntranceVertex.getOutgoing().size());
      assertEquals(vertexNumber, vehicleParkingEntranceVertex.getIncoming().size());

      for (var edge : vehicleParkingEntranceVertex.getOutgoing()) {
        assertTrue(edge instanceof VehicleParkingEdge);
      }

      for (var edge : vehicleParkingEntranceVertex.getIncoming()) {
        assertTrue(edge instanceof VehicleParkingEdge);
      }
    }
  }
}
