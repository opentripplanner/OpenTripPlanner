package org.opentripplanner.graph_builder.module.islandpruning;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.module.islandpruning.IslandPruningUtils.buildOsmGraph;

import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.test.support.ResourceLoader;

/**
 * Test data consists of one bigger graph and two small sub graphs. These are totally disconnected.
 * One small graphs is only at 5 meter distance from the big graph and another one 30 m away.
 * Adaptive pruning retains the distant island but removes the closer one which appears to be
 * disconnected part of the main graph.
 */
public class AdaptivePruningTest {

  private static Graph graph;

  @BeforeAll
  static void setup() {
    graph = buildOsmGraph(
      ResourceLoader.of(AdaptivePruningTest.class).file("isoiiluoto.pbf"),
      5,
      0,
      20,
      30
    );
  }

  @Test
  public void distantIslandIsRetained() {
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
  public void nearIslandIsRemoved() {
    Assertions.assertFalse(
      graph
        .getStreetEdges()
        .stream()
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .contains("37751757")
    );
  }

  @Test
  public void mainGraphIsNotRemoved() {
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
