package org.opentripplanner.graph_builder.module.osm.moduletests.parking;

import static com.google.common.truth.Truth.assertThat;
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

/// Tests that a bike parking lot with no boundary node shared with any road gets an artificial
/// centroid entrance so the routing algorithm can reach it.
class DisconnectedBikeParkingTest {

  /// Unlike car parking, no issue is emitted for disconnected bike parking, because the majority of
  /// bike facilities are not connected to the street network.
  @Test
  void disconnectedBikeParkingGetsArtificialCentroidEntrance() {
    var n1 = OsmNode.of().withId(1).withLatLon(0.0, 0.0).build();
    var n2 = OsmNode.of().withId(2).withLatLon(0.001, 0.0).build();
    var n3 = OsmNode.of().withId(3).withLatLon(0.001, 0.001).build();
    var n4 = OsmNode.of().withId(4).withLatLon(0.0, 0.001).build();

    var bikeParkingArea = OsmWay.of()
      .withId(1)
      .withTag("amenity", "bicycle_parking")
      .withTag("name", "Test Bike Parking")
      .addNodeRef(1, 2, 3, 4, 1)
      .build();

    var provider = new TestOsmProvider(
      List.of(),
      List.of(bikeParkingArea),
      List.of(n1, n2, n3, n4)
    );

    var graph = new Graph();
    var issueStore = new DefaultDataImportIssueStore();

    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withIssueStore(issueStore)
      .withStaticBikeParkAndRide(true)
      .build()
      .buildGraph();

    var fetcher = new GraphSummarizer(graph);

    assertWithMessage("Unexpected edges. Check graph at %s", fetcher.geoJsonUrl())
      .that(fetcher.summarizeEdges())
      .containsExactly(
        "Parking (0.0005,0.0005)[Vehicle parking OSM:OsmWay/1/centroid] → (0.0005,0.0005)[Vehicle parking OSM:OsmWay/1/centroid]"
      );

    assertThat(issueStore.listIssues()).isEmpty();
  }
}
