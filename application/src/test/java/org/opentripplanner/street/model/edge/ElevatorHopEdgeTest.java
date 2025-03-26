package org.opentripplanner.street.model.edge;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.module.osm.moduletests._support.NodeBuilder.node;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.graph_builder.module.osm.moduletests._support.TestOsmProvider;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.Deduplicator;

class ElevatorHopEdgeTest {

  Vertex from = intersectionVertex(0, 0);
  Vertex to = intersectionVertex(1, 1);

  static Stream<Arguments> noTraverse() {
    return Stream.of(Accessibility.NO_INFORMATION, Accessibility.NOT_POSSIBLE).map(Arguments::of);
  }

  @ParameterizedTest(name = "{0} should be allowed to traverse when requesting onlyAccessible")
  @MethodSource("noTraverse")
  void shouldNotTraverse(Accessibility wheelchair) {
    var req = StreetSearchRequest.of();
    AccessibilityPreferences feature = AccessibilityPreferences.ofOnlyAccessible();
    req
      .withWheelchair(true)
      .withPreferences(preferences ->
        preferences.withWheelchair(
          WheelchairPreferences.of()
            .withTrip(feature)
            .withStop(feature)
            .withElevator(feature)
            .withInaccessibleStreetReluctance(25)
            .withMaxSlope(0.5)
            .withSlopeExceededReluctance(10)
            .withStairsReluctance(25)
            .build()
        )
      );

    var result = traverse(wheelchair, req.build());
    assertTrue(State.isEmpty(result));
  }

  static Stream<Arguments> all() {
    return Stream.of(
      // no extra cost
      Arguments.of(Accessibility.POSSIBLE, 20),
      // low extra cost
      Arguments.of(Accessibility.NO_INFORMATION, 40),
      // high extra cost
      Arguments.of(Accessibility.NOT_POSSIBLE, 3620)
    );
  }

  @ParameterizedTest(name = "{0} should allowed to traverse with a cost of {1}")
  @MethodSource("all")
  void allowByDefault(Accessibility wheelchair, double expectedCost) {
    var req = StreetSearchRequest.of().build();
    var result = traverse(wheelchair, req)[0];
    assertNotNull(result);
    assertTrue(result.weight > 1);

    req = StreetSearchRequest.copyOf(req).withWheelchair(true).build();
    var wheelchairResult = traverse(wheelchair, req)[0];
    assertNotNull(wheelchairResult);
    assertEquals(expectedCost, wheelchairResult.weight);
  }

  private State[] traverse(Accessibility wheelchair, StreetSearchRequest req) {
    var edge = ElevatorHopEdge.createElevatorHopEdge(
      from,
      to,
      StreetTraversalPermission.ALL,
      wheelchair
    );
    var state = new State(from, req);

    return edge.traverse(state);
  }

  @Test
  void testDuration() {
    var way = new OsmWay();
    way.addTag("duration", "00:01:02");
    way.addTag("highway", "elevator");
    var provider = TestOsmProvider.of().addWay(way).build();
    var graph = new Graph(new Deduplicator());
    var osmModule = OsmModule.of(
      provider,
      graph,
      new DefaultOsmInfoGraphBuildRepository(),
      new DefaultVehicleParkingRepository()
    ).build();
    osmModule.buildGraph();
    var edges = graph.getEdgesOfType(ElevatorHopEdge.class);
    assertThat(edges).hasSize(2);
    for (var edge : edges) {
      assertThat(edge.getTravelTime()).isEqualTo(62);
    }
  }

  @Test
  void testMultilevelNodeDuration() {
    var node0 = node(0, new WgsCoordinate(0, 0));
    var node1 = node(1, new WgsCoordinate(2, 0));
    var node = node(2, new WgsCoordinate(1, 0));
    node.addTag("duration", "00:01:02");
    node.addTag("highway", "elevator");
    node.addTag("level", "1;2");
    var provider = TestOsmProvider.of()
      .addWayFromNodes(way -> way.addTag("level", "1"), node0, node)
      .addWayFromNodes(way -> way.addTag("level", "2"), node1, node)
      .build();
    var graph = new Graph(new Deduplicator());
    var osmModule = OsmModule.of(
      provider,
      graph,
      new DefaultOsmInfoGraphBuildRepository(),
      new DefaultVehicleParkingRepository()
    ).build();
    osmModule.buildGraph();
    var edges = graph.getEdgesOfType(ElevatorHopEdge.class);
    assertThat(edges).hasSize(2);
    for (var edge : edges) {
      assertThat(edge.getTravelTime()).isEqualTo(62);
    }
  }

  @Test
  void testTraversal() {
    var edge = ElevatorHopEdge.createElevatorHopEdge(
      from,
      to,
      StreetTraversalPermission.ALL,
      null,
      2,
      62
    );
    var req = StreetSearchRequest.of().withMode(StreetMode.WALK);
    var res = edge.traverse(new State(from, req.build()))[0];
    assertThat(res.getTimeDeltaMilliseconds()).isEqualTo(62_000);
  }
}
