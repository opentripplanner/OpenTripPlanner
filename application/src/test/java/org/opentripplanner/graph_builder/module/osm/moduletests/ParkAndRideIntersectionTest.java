package org.opentripplanner.graph_builder.module.osm.moduletests;

import static com.google.common.truth.Truth.assertWithMessage;
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
import org.opentripplanner.street.graph.GraphDataFetcher;

/// Tests that a P&R areas that are intersected by other ways, which share no nodes, should
/// create synthetic nodes to connect the P&R area to the street network.
///
/// This is quite a dubious feature that I would like to revisit.
class ParkAndRideIntersectionTest {

  @Test
  void wayCrossingPR() {
    var n1 = OsmNode.of().withId(1).withLatLon(0.0, 0.0).build();
    var n2 = OsmNode.of().withId(2).withLatLon(0.001, 0.0).build();
    var n3 = OsmNode.of().withId(3).withLatLon(0.001, 0.001).build();
    var n4 = OsmNode.of().withId(4).withLatLon(0.0, 0.001).build();
    var n5 = OsmNode.of().withId(5).withLatLon(0.0, -0.001).build();
    var n6 = OsmNode.of().withId(6).withLatLon(0.001, 0.002).build();

    var parkingArea = OsmWay.of()
      .withId(1)
      .withTag("amenity", "parking")
      .withTag("park_ride", "yes")
      .withTag("name", "Test P+R")
      .addNodeRef(1, 2, 3, 4, 1)
      .build();

    var serviceRoad = OsmWay.of().withId(2).withTag("highway", "service").addNodeRef(5, 6).build();

    var provider = new TestOsmProvider(
      List.of(),
      List.of(parkingArea, serviceRoad),
      List.of(n1, n2, n3, n4, n5, n6)
    );

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

    var fetcher = new GraphDataFetcher(graph);

    assertWithMessage("Unexpected edges. Check graph at %s", fetcher.geoJsonUrl())
      .that(fetcher.summarizeEdges())
      .containsExactly(
        "(0.000333,0) → (0,-0.001) ALL ♿✅",
        "(0.000333,0) → (0.000667,0.001) ALL ♿✅",
        "(0,-0.001) → (0.000333,0) ALL ♿✅",
        "(0.000667,0.001) → (0.000333,0) ALL ♿✅",
        "(0.001,0.002) → (0.000667,0.001) ALL ♿✅",
        "(0.000667,0.001) → (0.001,0.002) ALL ♿✅",
        "Parking (0.000667,0.001)[Vehicle parking OSM:OsmWay/1/osm:node:-100000] → (0.000667,0.001)[Vehicle parking OSM:OsmWay/1/osm:node:-100000]",
        "Parking (0.000667,0.001)[Vehicle parking OSM:OsmWay/1/osm:node:-100000] → (0.000333,0)[Vehicle parking OSM:OsmWay/1/osm:node:-100001]",
        "Parking (0.000333,0)[Vehicle parking OSM:OsmWay/1/osm:node:-100001] → (0.000667,0.001)[Vehicle parking OSM:OsmWay/1/osm:node:-100000]",
        "Parking (0.000333,0)[Vehicle parking OSM:OsmWay/1/osm:node:-100001] → (0.000333,0)[Vehicle parking OSM:OsmWay/1/osm:node:-100001]"
      );
  }
}
