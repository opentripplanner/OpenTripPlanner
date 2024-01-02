package org.opentripplanner.graph_builder.module.islandpruning;

import static org.opentripplanner.graph_builder.module.islandpruning.IslandPruningUtils.buildOsmGraph;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.test.support.ResourceLoader;

public class EscalatorPruningTest {

  private static Graph graph;

  @Test
  public void streetEdgesBetweenEscalatorEdgesRetained() {
    graph =
      buildOsmGraph(ResourceLoader.of(EscalatorPruningTest.class).file("matinkyla-escalator.pbf"));
    Assertions.assertTrue(
      graph
        .getStreetEdges()
        .stream()
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .containsAll(Set.of("490072445"))
    );
  }
}
