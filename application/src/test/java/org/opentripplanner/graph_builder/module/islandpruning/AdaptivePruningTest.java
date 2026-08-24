package org.opentripplanner.graph_builder.module.islandpruning;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.module.islandpruning.IslandPruningUtils.buildOsmGraph;

import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.test.support.ResourceLoader;

/**
 * Test data consists of one bigger graph and two small sub graphs. These are totally disconnected.
 * One small graphs is only at 5 meter distance from the big graph and another one 30 m away.
 * Adaptive pruning retains the distant island but removes the closer one which appears to be
 * disconnected part of the main graph.
 */
class AdaptivePruningTest {

  private static Graph graph;

  @BeforeAll
  static void setup() {
    graph = buildOsmGraph(
      ResourceLoader.of(AdaptivePruningTest.class).file("isoiiluoto.pbf"),
      IslandPruningParameters.of()
        .withPruningThresholdIslandWithoutStops(5)
        .withPruningThresholdIslandWithStops(0)
        .withAdaptivePruningFactor(20)
        .withAdaptivePruningDistance(30)
        .build()
    );
  }

  @Test
  void distantIslandIsRetained() {
    assertTrue(
      graph
        .getStreetEdges()
        .stream()
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .contains("73386383")
    );
  }

  @Test
  void nearIslandIsRemoved() {
    assertFalse(
      graph
        .getStreetEdges()
        .stream()
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .contains("37751757")
    );
  }

  @Test
  void mainGraphIsNotRemoved() {
    assertTrue(
      graph
        .getStreetEdges()
        .stream()
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .contains("73347312")
    );
  }
}
