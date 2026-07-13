package org.opentripplanner.graph_builder.module.osm.moduletests.parking;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.summary.GraphSummarizer;

/// Tests that an entrance vertex is created for parking lots that are not connected to the rest
/// of the street network.
class UnconnectedParkAndRideTest {

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
        "(0,-0.001) → (0.001,0.002) ALL ♿✅",
        "(0.001,0.002) → (0,-0.001) ALL ♿✅",
        "Parking (0.0005,0.0005)[Vehicle parking OSM:OsmWay/1/centroid] → (0.0005,0.0005)[Vehicle parking OSM:OsmWay/1/centroid]"
      );

    assertThat(issueStore.listIssues().stream().map(DataImportIssue::getType)).containsExactly(
      "IsolatedParkAndRide"
    );
  }
}
