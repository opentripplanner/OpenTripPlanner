package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.OsmBoardingLocationVertex;

class OsmBoardingLocationsModuleTest {

  File file = new File(ConstantsForTests.HERRENBERG_OSM);

  @Test
  void extractBoardingLocations() {
    var graph = new Graph();
    var extra = new HashMap<Class<?>, Object>();

    var provider = new OpenStreetMapProvider(file, false);

    var osmModule = new OpenStreetMapModule(List.of(provider), Set.of("ref", "ref:IFOPT"));

    osmModule.buildGraph(graph, extra);

    var boardingLocationsModule = new OsmBoardingLocationsModule();
    boardingLocationsModule.buildGraph(graph, extra);

    var boardingLocations = graph.getVerticesOfType(OsmBoardingLocationVertex.class);
    assertEquals(5, boardingLocations.size());
    System.out.println(boardingLocations);
  }
}
