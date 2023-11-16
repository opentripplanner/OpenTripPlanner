package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingHelper;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingTestGraphData;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.VertexFactory;
import org.opentripplanner.transit.service.TransitModel;

public class VehicleParkingLinkingTest {

  private Graph graph;
  private TransitModel transitModel;
  private IntersectionVertex A;
  private IntersectionVertex B;

  private VertexFactory vertexFactory;

  private VehicleParkingHelper helper;

  @BeforeEach
  public void setup() {
    VehicleParkingTestGraphData graphData = new VehicleParkingTestGraphData();
    graphData.initGraph();
    graph = graphData.getGraph();
    transitModel = graphData.getTransitModel();
    A = graphData.getAVertex();
    B = graphData.getBVertex();
    vertexFactory = new VertexFactory(graph);
    helper = new VehicleParkingHelper(graph);
  }

  @Test
  public void entranceWithVertexLinkingTest() {
    var parking = VehicleParking
      .builder()
      .entrance(builder ->
        builder.entranceId(id("1")).coordinate(new WgsCoordinate(A.getCoordinate())).vertex(A)
      )
      .build();
    var parkingVertex = vertexFactory.vehicleParkingEntrance(parking);

    TestStreetLinkerModule.link(graph, transitModel);

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
        builder
          .entranceId(id("1"))
          .coordinate(new WgsCoordinate(0, 0.0001))
          .carAccessible(true)
          .walkAccessible(true)
      )
      .build();
    var parkingVertex = vertexFactory.vehicleParkingEntrance(parking.getEntrances().get(0));

    TestStreetLinkerModule.link(graph, transitModel);

    var streetLinks = graph.getEdgesOfType(StreetVehicleParkingLink.class);
    assertEquals(2, streetLinks.size());

    streetLinks.forEach(e ->
      assertTrue(e.getFromVertex().equals(parkingVertex) ^ e.getToVertex().equals(parkingVertex))
    );
  }

  @Test
  public void carParkingEntranceToAllTraversableStreetLinkingTest() {
    var C = StreetModelForTest.intersectionVertex("C", 0.0001, 0.0001);
    var D = StreetModelForTest.intersectionVertex("D", 0.01, 0.01);

    graph.addVertex(C);
    graph.addVertex(D);

    StreetModelForTest.streetEdge(C, D, StreetTraversalPermission.CAR);

    StreetModelForTest.streetEdge(A, C, StreetTraversalPermission.NONE);

    var parking = VehicleParking
      .builder()
      .entrance(builder ->
        builder
          .entranceId(id("1"))
          .coordinate(new WgsCoordinate(0, 0.0001))
          .carAccessible(true)
          .walkAccessible(true)
      )
      .build();
    var parkingVertex = vertexFactory.vehicleParkingEntrance(parking.getEntrances().get(0));

    TestStreetLinkerModule.link(graph, transitModel);

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
      .entrance(builder ->
        builder
          .entranceId(id("Entrance-1"))
          .coordinate(new WgsCoordinate(A.getCoordinate()))
          .vertex(A)
          .walkAccessible(true)
      )
      .entrance(builder ->
        builder
          .entranceId(id("Entrance-2"))
          .coordinate(new WgsCoordinate(B.getCoordinate()))
          .vertex(B)
          .walkAccessible(true)
      )
      .build();

    helper.linkVehicleParkingToGraph(vehicleParking);

    graph.remove(A);

    TestStreetLinkerModule.link(graph, transitModel);

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
      .entrance(builder ->
        builder
          .entranceId(id("Entrance-1"))
          .coordinate(new WgsCoordinate(A.getCoordinate()))
          .vertex(A)
          .walkAccessible(true)
      )
      .build();

    var vehicleParkingService = graph.getVehicleParkingService();
    vehicleParkingService.updateVehicleParking(List.of(vehicleParking), List.of());
    helper.linkVehicleParkingToGraph(vehicleParking);

    graph.remove(A);

    TestStreetLinkerModule.link(graph, transitModel);

    assertEquals(0, graph.getVerticesOfType(VehicleParkingEntranceVertex.class).size());

    assertEquals(0, graph.getEdgesOfType(VehicleParkingEdge.class).size());
    assertEquals(0, graph.getEdgesOfType(StreetVehicleParkingLink.class).size());

    assertEquals(0, graph.getEdgesOfType(StreetVehicleParkingLink.class).size());
    assertEquals(0, vehicleParkingService.getVehicleParkings().count());
  }
}
