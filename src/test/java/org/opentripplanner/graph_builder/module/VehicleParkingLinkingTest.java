package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.StreetVehicleParkingLink;
import org.opentripplanner.routing.edgetype.VehicleParkingEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingHelper;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingTestGraphData;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingTestUtil;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.VehicleParkingEntranceVertex;
import org.opentripplanner.transit.service.TransitModel;

public class VehicleParkingLinkingTest {

  private Graph graph;
  private TransitModel transitModel;
  private IntersectionVertex A;
  private IntersectionVertex B;

  @BeforeEach
  public void setup() {
    VehicleParkingTestGraphData graphData = new VehicleParkingTestGraphData();
    graphData.initGraph();
    graph = graphData.getGraph();
    transitModel = graphData.getTransitModel();
    A = graphData.getAVertex();
    B = graphData.getBVertex();
  }

  @Test
  public void entranceWithVertexLinkingTest() {
    var parking = VehicleParking
      .builder()
      .entrance(builder -> builder.entranceId(id("1")).vertex(A))
      .build();
    var parkingVertex = new VehicleParkingEntranceVertex(graph, parking.getEntrances().get(0));

    StreetLinkerModule.linkStreetsForTestOnly(graph, transitModel);

    assertEquals(1, parkingVertex.getOutgoing().size());
    parkingVertex.getOutgoing().forEach(e -> assertEquals(e.getToVertex(), A));

    assertEquals(1, parkingVertex.getIncoming().size());
    parkingVertex.getIncoming().forEach(e -> assertEquals(e.getFromVertex(), A));
  }

  @Test
  public void entranceWithoutVertexLinkingTest() {
    var parking = VehicleParking
      .builder()
      .entrance(builder ->
        builder.entranceId(id("1")).x(0.0001).y(0).carAccessible(true).walkAccessible(true)
      )
      .build();
    var parkingVertex = new VehicleParkingEntranceVertex(graph, parking.getEntrances().get(0));

    StreetLinkerModule.linkStreetsForTestOnly(graph, transitModel);

    var streetLinks = graph.getEdgesOfType(StreetVehicleParkingLink.class);
    assertEquals(2, streetLinks.size());

    streetLinks.forEach(e ->
      assertTrue(e.getFromVertex().equals(parkingVertex) ^ e.getToVertex().equals(parkingVertex))
    );
  }

  @Test
  public void carParkingEntranceToAllTraversableStreetLinkingTest() {
    var C = new IntersectionVertex(graph, "C", 0.0001, 0.0001);
    var D = new IntersectionVertex(graph, "D", 0.01, 0.01);
    VehicleParkingTestUtil.createStreet(C, D, StreetTraversalPermission.CAR);

    VehicleParkingTestUtil.createStreet(A, C, StreetTraversalPermission.NONE);

    var parking = VehicleParking
      .builder()
      .entrance(builder ->
        builder.entranceId(id("1")).x(0.0001).y(0).carAccessible(true).walkAccessible(true)
      )
      .build();
    var parkingVertex = new VehicleParkingEntranceVertex(graph, parking.getEntrances().get(0));

    StreetLinkerModule.linkStreetsForTestOnly(graph, transitModel);

    var streetLinks = graph.getEdgesOfType(StreetVehicleParkingLink.class);
    assertEquals(4, streetLinks.size());

    streetLinks.forEach(e ->
      assertTrue(e.getFromVertex().equals(parkingVertex) ^ e.getToVertex().equals(parkingVertex))
    );
  }

  @Test
  public void removeEntranceWithNonExistingVertexTest() {
    var vehicleParking = VehicleParking
      .builder()
      .id(id("VP"))
      .bicyclePlaces(true)
      .entrance(builder -> builder.entranceId(id("Entrance-1")).vertex(A).walkAccessible(true))
      .entrance(builder -> builder.entranceId(id("Entrance-2")).vertex(B).walkAccessible(true))
      .build();

    VehicleParkingHelper.linkVehicleParkingToGraph(graph, vehicleParking);

    graph.remove(A);

    StreetLinkerModule.linkStreetsForTestOnly(graph, transitModel);

    assertEquals(1, vehicleParking.getEntrances().size());

    assertEquals(1, graph.getVerticesOfType(VehicleParkingEntranceVertex.class).size());

    assertEquals(1, graph.getEdgesOfType(VehicleParkingEdge.class).size());
    assertEquals(2, graph.getEdgesOfType(StreetVehicleParkingLink.class).size());
  }

  @Test
  public void removeVehicleParkingWithOneEntranceAndNonExistingVertexTest() {
    var vehicleParking = VehicleParking
      .builder()
      .id(id("VP"))
      .bicyclePlaces(true)
      .entrance(builder -> builder.entranceId(id("Entrance-1")).vertex(A).walkAccessible(true))
      .build();

    var vehicleParkingService = graph.getVehicleParkingService();
    vehicleParkingService.addVehicleParking(vehicleParking);
    VehicleParkingHelper.linkVehicleParkingToGraph(graph, vehicleParking);

    graph.remove(A);

    StreetLinkerModule.linkStreetsForTestOnly(graph, transitModel);

    assertEquals(0, graph.getVerticesOfType(VehicleParkingEntranceVertex.class).size());

    assertEquals(0, graph.getEdgesOfType(VehicleParkingEdge.class).size());
    assertEquals(0, graph.getEdgesOfType(StreetVehicleParkingLink.class).size());

    assertEquals(0, graph.getEdgesOfType(StreetVehicleParkingLink.class).size());
    assertEquals(0, vehicleParkingService.getVehicleParkings().count());
  }
}
