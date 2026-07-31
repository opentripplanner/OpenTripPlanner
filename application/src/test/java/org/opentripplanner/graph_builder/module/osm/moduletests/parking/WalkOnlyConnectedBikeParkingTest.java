package org.opentripplanner.graph_builder.module.osm.moduletests.parking;

import static com.google.common.truth.Truth.assertWithMessage;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.summary.GraphSummarizer;

/// Tests that a bike parking lot connected to the street network only via walk-accessible edges
/// uses the walk entrance directly — no artificial centroid entrance is needed.
class WalkOnlyConnectedBikeParkingTest {

  /// A bike parking lot whose boundary shares a node with a footway is reachable on foot, which is
  /// sufficient for bike parking. The walk-accessible entrance should be used as-is, without
  /// creating an artificial centroid entrance.
  @Test
  void walkOnlyConnectedBikeParkingUsesWalkEntrance() {
    var n1 = OsmNode.of().withId(1).withLatLon(0.0, 0.0).build();
    var n2 = OsmNode.of().withId(2).withLatLon(0.001, 0.0).build();
    var n3 = OsmNode.of().withId(3).withLatLon(0.001, 0.001).build();
    var n4 = OsmNode.of().withId(4).withLatLon(0.0, 0.001).build();
    var n5 = OsmNode.of().withId(5).withLatLon(0.0, -0.001).build();

    var bikeParkingArea = OsmWay.of()
      .withId(1)
      .withTag("amenity", "bicycle_parking")
      .withTag("name", "Test Bike Parking")
      .addNodeRef(1, 2, 3, 4, 1)
      .build();

    var footway = OsmWay.of().withId(2).withTag("highway", "footway").addNodeRef(5, 1).build();

    var provider = new TestOsmProvider(
      List.of(),
      List.of(bikeParkingArea, footway),
      List.of(n1, n2, n3, n4, n5)
    );

    var graph = new Graph();

    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withStaticBikeParkAndRide(true)
      .build()
      .buildGraph();

    var fetcher = new GraphSummarizer(graph);

    assertWithMessage("Unexpected edges. Check graph at %s", fetcher.geoJsonUrl())
      .that(fetcher.summarizeEdges())
      .containsExactly(
        "(0,-0.001) → (0,0) PEDESTRIAN ♿✅",
        "(0,0) → (0,-0.001) PEDESTRIAN ♿✅",
        // walk entrance at n1 is sufficient — no artificial centroid entrance needed
        "Parking (0,0)[Vehicle parking OSM:OsmWay/1/osm:node:1] → (0,0)[Vehicle parking OSM:OsmWay/1/osm:node:1]"
      );
  }
}
