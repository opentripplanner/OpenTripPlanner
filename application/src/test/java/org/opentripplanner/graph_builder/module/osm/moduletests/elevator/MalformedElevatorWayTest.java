package org.opentripplanner.graph_builder.module.osm.moduletests.elevator;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.osm.model.NodeBuilder.node;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.issues.FewerThanTwoIntersectionNodesInElevatorWay;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.NodeBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.summary.GraphSummarizer;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;

class MalformedElevatorWayTest {

  /**
   * If the connected nodes of an elevator way have been modeled as elevators, they do not appear
   * as intersection nodes. OTP should create an issue, but it should not cause errors.
   */
  @Test
  void elevatorWayWithFewerThanTwoIntersectionNodes() {
    var n1 = NodeBuilder.of(1, new WgsCoordinate(1, 1)).withTag("highway", "elevator").build();
    var n2 = NodeBuilder.of(2, new WgsCoordinate(2, 2)).withTag("highway", "elevator").build();

    var provider = TestOsmProvider.of()
      .addWayFromNodes(way -> way.withTag("highway", "elevator"), n1, n2)
      .build();
    var graph = new Graph();
    var issueStore = new DefaultDataImportIssueStore();

    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withIssueStore(issueStore)
      .build()
      .buildGraph();

    var elevatorHopEdges = graph.findEdges(ElevatorHopEdge.class);
    assertThat(elevatorHopEdges).hasSize(0);

    var issues = issueStore
      .listIssues()
      .stream()
      .filter(issue -> issue instanceof FewerThanTwoIntersectionNodesInElevatorWay)
      .map(FewerThanTwoIntersectionNodesInElevatorWay.class::cast)
      .toList();
    assertEquals(1, issues.size());
    assertEquals(0, issues.getFirst().intersectionNodes());
  }

  /**
   * Regression test for a NullPointerException reported after #7905 ("Speed up OSM way
   * processing"): an elevator way endpoint can be recorded as a candidate intersection node
   * (because it is referenced by a second way) without ever getting a real graph vertex built for
   * it.
   * <p>
   * In this test, node {@code b} sits at the same coordinate as the elevator way's other endpoint
   * {@code a} - a mapping error rather than a legitimate way of tagging an elevator, but one that
   * does occur in real OSM data - and carries no node-level "level" tag itself, so the
   * duplicate-node handling in OsmModule's street-graph-building loop drops it while processing the
   * elevator way: it never becomes the "from" or "to" node of a street edge there. Node {@code b}
   * is also referenced by a second way that is relevant for routing (it's a bicycle parking way)
   * but not "routable" in the street-graph sense, so OsmModule's street-graph-building loop skips
   * that way entirely too. As a result no vertex is ever created for {@code b}, even though it was
   * recorded as a candidate intersection node while scanning for nodes shared between ways -
   * exactly the combination that made {@code ElevatorProcessor} crash: it found {@code b} in its
   * list of candidate intersection nodes for the elevator way, then failed to look up a vertex that
   * was never built.
   * <p>
   * The vertex-generator's intersection-node check must distinguish "candidate, not yet built" from
   * "has a real vertex" so that elevator-way processing degrades gracefully (reporting a
   * {@link FewerThanTwoIntersectionNodesInElevatorWay} issue) instead of crashing with a
   * NullPointerException when it looks up the vertex.
   */
  @Test
  void elevatorWayEndpointSharedWithNonRoutableWay() {
    var a = node(1, new WgsCoordinate(1, 1));
    // `b` has the same coordinate as `a` and no level tag of its own
    var b = node(2, new WgsCoordinate(1, 1));
    var c = node(3, new WgsCoordinate(2, 2));
    var d = node(4, new WgsCoordinate(3, 3));

    var provider = TestOsmProvider.of()
      .addWayFromNodes(way -> way.withTag("highway", "elevator").withTag("level", "0;-2"), a, b)
      .addWayFromNodes(way -> way.withTag("highway", "footway"), a, c)
      .addWayFromNodes(
        way -> way.withTag("highway", "construction").withTag("amenity", "bicycle_parking"),
        b,
        d
      )
      .build();
    var graph = new Graph();
    var issueStore = new DefaultDataImportIssueStore();

    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .builder()
      .withIssueStore(issueStore)
      .build()
      .buildGraph();

    var summarizer = new GraphSummarizer(graph);

    assertWithMessage("Expected no elevator hop edges. Check graph at %s", summarizer.geoJsonUrl())
      .that(summarizer.summarizeEdges())
      .containsExactly("(1,1) → (2,2) PEDESTRIAN ♿✅", "(2,2) → (1,1) PEDESTRIAN ♿✅");

    var issues = issueStore.listIssues().stream().map(DataImportIssue::getType);
    assertThat(issues).containsExactly("FewerThanTwoIntersectionNodesInElevatorWay");
  }
}
