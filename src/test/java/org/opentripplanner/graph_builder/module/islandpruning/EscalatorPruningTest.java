package org.opentripplanner.graph_builder.module.islandpruning;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.module.islandpruning.IslandPruningUtils.buildOsmGraph;

import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.test.support.ResourceLoader;

public class EscalatorPruningTest {

  @Test
  public void streetEdgesBetweenEscalatorEdgesRetained() {
    var graph = buildOsmGraph(
      ResourceLoader.of(EscalatorPruningTest.class).file("matinkyla-escalator.pbf"),
      10,
      2,
      50,
      250
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
