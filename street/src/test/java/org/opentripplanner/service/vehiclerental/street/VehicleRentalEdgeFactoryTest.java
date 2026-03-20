package org.opentripplanner.service.vehiclerental.street;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.RentalFormFactor.BICYCLE;
import static org.opentripplanner.street.model.RentalFormFactor.SCOOTER;

import org.junit.jupiter.api.Test;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.TestVehicleRentalStationBuilder;
import org.opentripplanner.street.Scope;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.DisposableEdgeCollection;
import org.opentripplanner.street.model.RentalFormFactor;

class VehicleRentalEdgeFactoryTest {

  @Test
  void createRentalEdgesForSingleFormFactor() {
    var station = TestVehicleRentalStationBuilder.of()
      .withVehicleTypeBicycle(3, 3)
      .withStationOn(true)
      .build();
    var vertex = new VehicleRentalPlaceVertex(station);
    var graph = new Graph();
    var edges = new DisposableEdgeCollection(graph, Scope.REALTIME);

    VehicleRentalEdge.createRentalEdgesForStation(vertex, station, edges);

    assertEquals(1, vertex.getOutgoing().size());
    var edge = (VehicleRentalEdge) vertex.getOutgoing().iterator().next();
    assertEquals(BICYCLE, edge.formFactor);
  }

  @Test
  void createRentalEdgesForMultipleFormFactors() {
    var station = TestVehicleRentalStationBuilder.of()
      .withVehicleTypeBicycle(3, 3)
      .withVehicleType(SCOOTER, RentalVehicleType.PropulsionType.ELECTRIC, 2, 2)
      .withStationOn(true)
      .build();
    var vertex = new VehicleRentalPlaceVertex(station);
    var graph = new Graph();
    var edges = new DisposableEdgeCollection(graph, Scope.REALTIME);

    VehicleRentalEdge.createRentalEdgesForStation(vertex, station, edges);

    assertEquals(2, vertex.getOutgoing().size());
    var formFactors = vertex
      .getOutgoing()
      .stream()
      .map(e -> ((VehicleRentalEdge) e).formFactor)
      .collect(java.util.stream.Collectors.toSet());
    assertTrue(formFactors.contains(BICYCLE));
    assertTrue(formFactors.contains(SCOOTER));
  }

  @Test
  void createRentalEdgesDeduplicatesOverlappingFormFactors() {
    // A station where BICYCLE is both a pickup and dropoff form factor
    var station = TestVehicleRentalStationBuilder.of()
      .withVehicleTypeBicycle(3, 3)
      .withStationOn(true)
      .build();
    var vertex = new VehicleRentalPlaceVertex(station);
    var graph = new Graph();
    var edges = new DisposableEdgeCollection(graph, Scope.REALTIME);

    VehicleRentalEdge.createRentalEdgesForStation(vertex, station, edges);

    // Should only create one edge per form factor, not duplicate for pickup + dropoff
    assertEquals(1, vertex.getOutgoing().size());
  }

  @Test
  void createRentalEdgesForStationWithNoFormFactors() {
    // Station with 0 available vehicles and 0 spaces — but still has a default type
    var station = TestVehicleRentalStationBuilder.of()
      .withVehicles(0)
      .withSpaces(0)
      .withStationOn(true)
      .build();
    var vertex = new VehicleRentalPlaceVertex(station);
    var graph = new Graph();
    var edges = new DisposableEdgeCollection(graph, Scope.REALTIME);

    VehicleRentalEdge.createRentalEdgesForStation(vertex, station, edges);

    // Default vehicle type is BICYCLE, even with 0 count it's still in the type map
    var formFactors = vertex
      .getOutgoing()
      .stream()
      .map(e -> ((VehicleRentalEdge) e).formFactor)
      .collect(java.util.stream.Collectors.toSet());
    assertTrue(formFactors.contains(RentalFormFactor.BICYCLE));
  }
}
