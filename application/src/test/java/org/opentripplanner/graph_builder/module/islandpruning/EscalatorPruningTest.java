package org.opentripplanner.graph_builder.module.islandpruning;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.module.islandpruning.IslandPruningUtils.buildOsmGraph;

import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.test.support.ResourceLoader;

class EscalatorPruningTest {

  @Test
  void streetEdgesBetweenEscalatorEdgesRetained() {
    var graph = buildOsmGraph(
      ResourceLoader.of(EscalatorPruningTest.class).file("matinkyla-escalator.pbf"),
      IslandPruningParameters.DEFAULTS
    );
    assertTrue(
      graph
        .getStreetEdges()
        .stream()
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .contains("490072445")
    );
  }
}
