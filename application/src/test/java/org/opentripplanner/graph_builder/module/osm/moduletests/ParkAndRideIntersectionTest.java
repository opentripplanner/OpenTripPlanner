package org.opentripplanner.graph_builder.module.osm.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.street.graph.Graph;

class ParkAndRideIntersectionTest {

  @Test
  void simpleSquareParkAndRideArea() {
    var n1 = OsmNode.of().withId(1).withLatLon(0.0, 0.0).build();
    var n2 = OsmNode.of().withId(2).withLatLon(0.001, 0.0).build();
    var n3 = OsmNode.of().withId(3).withLatLon(0.001, 0.001).build();
    var n4 = OsmNode.of().withId(4).withLatLon(0.0, 0.001).build();

    var parkingArea = OsmWay.of()
      .withId(1)
      .withTag("amenity", "parking")
      .withTag("park_ride", "yes")
      .withTag("name", "Test P+R")
      .addNodeRef(1, 2, 3, 4, 1)
      .build();

    var provider = new TestOsmProvider(List.of(), List.of(parkingArea), List.of(n1, n2, n3, n4));

    var graph = new Graph();
    var parkingRepository = new DefaultVehicleParkingRepository();

    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .withVehicleParkingRepository(parkingRepository)
      .builder()
      .withStaticParkAndRide(true)
      .build()
      .buildGraph();

    var parkings = List.copyOf(parkingRepository.listVehicleParkings());
    assertEquals(1, parkings.size());

    var parking = parkings.getFirst();
    assertTrue(parking.hasCarPlaces());
    assertEquals("Test P+R", parking.getName().toString());
  }
}
