package org.opentripplanner.graph_builder.module;

import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.services.osm.CustomNamer;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;

public class PruneNoThruIslandsTest {

  private static Graph graph;

  @Test
  public void bicycleNoThruIslandsBecomeNoThru() {
    Assertions.assertTrue(
      graph
        .getStreetEdges()
        .stream()
        .filter(StreetEdge::isBicycleNoThruTraffic)
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .containsAll(Set.of("159830262", "55735898", "159830266", "159830254"))
    );
  }

  @Test
  public void carNoThruIslandsBecomeNoThru() {
    Assertions.assertTrue(
      graph
        .getStreetEdges()
        .stream()
        .filter(StreetEdge::isMotorVehicleNoThruTraffic)
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .containsAll(Set.of("159830262", "55735898", "55735911"))
    );
  }

  @Test
  public void pruneFloatingBikeAndWalkIsland() {
    Assertions.assertFalse(
      graph
        .getStreetEdges()
        .stream()
        .map(streetEdge -> streetEdge.getName().toString())
        .collect(Collectors.toSet())
        .contains("159830257")
    );
  }

  @BeforeAll
  static void setup() {
    graph = buildOsmGraph(ConstantsForTests.ISLAND_PRUNE_OSM);
  }

  private static Graph buildOsmGraph(String osmPath) {
    try {
      var graph = new Graph();
      // Add street data from OSM
      File osmFile = new File(osmPath);
      OpenStreetMapProvider osmProvider = new OpenStreetMapProvider(osmFile, true);
      OpenStreetMapModule osmModule = new OpenStreetMapModule(osmProvider);
      osmModule.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
      osmModule.customNamer =
        new CustomNamer() {
          @Override
          public String name(OSMWithTags way, String defaultName) {
            return String.valueOf(way.getId());
          }

          @Override
          public void nameWithEdge(OSMWithTags way, StreetEdge edge) {}

          @Override
          public void postprocess(Graph graph) {}

          @Override
          public void configure() {}
        };
      osmModule.skipVisibility = true;
      osmModule.buildGraph(graph, new HashMap<>());
      // Prune floating islands and set noThru where necessary
      PruneNoThruIslands pruneNoThruIslands = new PruneNoThruIslands(null);
      pruneNoThruIslands.setPruningThresholdIslandWithoutStops(40);
      pruneNoThruIslands.setPruningThresholdIslandWithStops(5);
      pruneNoThruIslands.buildGraph(graph, new HashMap<>());

      return graph;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
