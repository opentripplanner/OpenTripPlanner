package org.opentripplanner.graph_builder.module;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.openstreetmap.BinaryOpenStreetMapProvider;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;

import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class PruneNoThruIslandsTest {
    static private Graph graph;

    @BeforeAll
    static void setup() {
        graph = buildOsmGraph(ConstantsForTests.ISLAND_PRUNE_OSM);
    }

    @Test
    public void bicycleNoThruIslandsBecomeNoThru() {
        Assertions.assertTrue(graph.getStreetEdges().stream()
                .filter(StreetEdge::isBicycleNoThruTraffic)
                .map(streetEdge -> streetEdge.wayId)
                .collect(Collectors.toSet()).containsAll(
                        Set.of(
                                159830262L,
                                55735898L,
                                159830266L,
                                159830254L
                        )
                ));
    }

    @Test
    public void carNoThruIslandsBecomeNoThru() {
        Assertions.assertTrue(graph.getStreetEdges().stream()
                .filter(StreetEdge::isMotorVehicleNoThruTraffic)
                .map(streetEdge -> streetEdge.wayId)
                .collect(Collectors.toSet()).containsAll(
                        Set.of(
                                159830262L,
                                55735898L,
                                55735911L
                        )
                ));
    }

    @Test
    public void pruneFloatingBikeAndWalkIsland() {
        Assertions.assertFalse(graph.getStreetEdges().stream()
                .map(streetEdge -> streetEdge.wayId)
                .collect(Collectors.toSet()).contains(159830257L));
    }

    private static Graph buildOsmGraph(String osmPath) {

        try {
            var graph = new Graph();
            // Add street data from OSM
            File osmFile = new File(osmPath);
            BinaryOpenStreetMapProvider osmProvider =
                    new BinaryOpenStreetMapProvider(osmFile, true);
            OpenStreetMapModule osmModule =
                    new OpenStreetMapModule(com.google.common.collect.Lists.newArrayList(osmProvider));
            osmModule.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
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
