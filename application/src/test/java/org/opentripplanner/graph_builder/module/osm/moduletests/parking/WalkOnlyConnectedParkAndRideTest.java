package org.opentripplanner.graph_builder.module.osm.moduletests.parking;

import static com.google.common.truth.Truth.assertWithMessage;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.summary.GraphSummarizer;

/// Tests that a car parking lot connected to the street network only via walk-accessible edges
/// receives an artificial car-accessible entrance.
class WalkOnlyConnectedParkAndRideTest {

  /// A car parking lot whose boundary shares a node with a footway is connected to the street
  /// network for pedestrians but not for cars.
  /// The algorithm must detect the missing car access and add an artificial car-accessible entrance
  /// at the lot's centroid, just as it does for a fully disconnected lot.
  @Test
  void walkOnlyConnectedParkingLotGetsArtificialCarEntrance() {
    var n1 = OsmNode.of().withId(1).withLatLon(0.0, 0.0).build();
    var n2 = OsmNode.of().withId(2).withLatLon(0.001, 0.0).build();
    var n3 = OsmNode.of().withId(3).withLatLon(0.001, 0.001).build();
    var n4 = OsmNode.of().withId(4).withLatLon(0.0, 0.001).build();
    var n5 = OsmNode.of().withId(5).withLatLon(0.0, -0.001).build();

    var parkingArea = OsmWay.of()
      .withId(1)
      .withTag("amenity", "parking")
      .withTag("park_ride", "yes")
      .withTag("name", "Test P+R")
      .addNodeRef(1, 2, 3, 4, 1)
      .build();

    // Walk-only footway sharing node n1 with the parking lot boundary
    var footway = OsmWay.of().withId(2).withTag("highway", "footway").addNodeRef(5, 1).build();

    var provider = new TestOsmProvider(
      List.of(),
      List.of(parkingArea, footway),
      List.of(n1, n2, n3, n4, n5)
    );

    var graph = new Graph();
    var issueStore = new DefaultDataImportIssueStore();

    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withIssueStore(issueStore)
      .withStaticParkAndRide(true)
      .build()
      .buildGraph();

    var fetcher = new GraphSummarizer(graph);

    assertWithMessage("Unexpected edges. Check graph at %s", fetcher.geoJsonUrl())
      .that(fetcher.summarizeEdges())
      .containsExactly(
        "(0,-0.001) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (0,-0.001) PEDESTRIAN ♿✅",
        // centroid that is later linked to the car-accessible street network
        "Parking (0.0005,0.0005)[Vehicle parking OSM:OsmWay/1/centroid] → (0.0005,0.0005)[Vehicle parking OSM:OsmWay/1/centroid]"
      );
  }
}
