package org.opentripplanner.graph_builder.module.islandpruning;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.module.islandpruning.IslandPruningUtils.buildOsmGraph;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.graph.summary.GraphSummarizer;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.test.support.ResourceLoader;

class PruneNoThruIslandsTest {

  private static GraphSummarizer graph;

  @BeforeAll
  static void setup() {
    graph = buildOsmGraph(
      ResourceLoader.of(PruneNoThruIslandsTest.class).file(
        "herrenberg-island-prune-nothru.osm.pbf"
      ),
      IslandPruningParameters.DEFAULTS
    );
  }

  @Test
  void bicycleIslandsBecomeNoThru() {
    assertTrue(
      graph
        .listStreetEdges()
        .stream()
        .filter(StreetEdge::isBicycleNoThruTraffic)
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .containsAll(Set.of("159830262", "55735898", "159830266", "159830254"))
    );
  }

  @Test
  void carIslandsBecomeNoThru() {
    assertTrue(
      graph
        .listStreetEdges()
        .stream()
        .filter(StreetEdge::isMotorVehicleNoThruTraffic)
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .containsAll(Set.of("159830262", "55735911"))
    );
  }

  @Test
  void pruneFloatingBikeAndWalkIsland() {
    assertFalse(
      graph
        .listStreetEdges()
        .stream()
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .contains("159830257")
    );
  }
}
