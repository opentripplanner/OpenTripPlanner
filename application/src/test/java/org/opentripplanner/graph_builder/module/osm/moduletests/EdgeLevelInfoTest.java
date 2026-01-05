package org.opentripplanner.graph_builder.module.osm.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.module.osm.moduletests._support.NodeBuilder.node;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.graph_builder.module.osm.moduletests._support.TestOsmProvider;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.streetdetails.StreetDetailsRepository;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.streetdetails.model.InclinedEdgeLevelInfo;
import org.opentripplanner.service.streetdetails.model.Level;
import org.opentripplanner.service.streetdetails.model.VertexLevelInfo;

public class EdgeLevelInfoTest {

  @Test
  void testInclinedEdgeLevelInfo() {
    var levelStairs = new OsmWay();
    levelStairs.setId(1);
    levelStairs.addTag("highway", "steps");
    levelStairs.addTag("incline", "up");
    levelStairs.addTag("level", "1;2");

    var inclineStairs = new OsmWay();
    inclineStairs.setId(2);
    inclineStairs.addTag("highway", "steps");
    inclineStairs.addTag("incline", "up");

    var escalator = new OsmWay();
    escalator.setId(3);
    escalator.addTag("highway", "steps");
    escalator.addTag("conveying", "yes");
    escalator.addTag("level", "1;-1");
    escalator.addTag("level:ref", "1;P1");

    var provider = TestOsmProvider.of()
      .addWay(levelStairs)
      .addWay(inclineStairs)
      .addWay(escalator)
      .build();
    var graph = new Graph();
    var streetDetailsRepository = new DefaultStreetDetailsRepository();
    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .withStreetDetailsRepository(streetDetailsRepository)
      .builder()
      // The build config field that needs to bet set for inclined edge level info to be stored.
      .withIncludeInclinedEdgeLevelInfo(true)
      .build()
      .buildGraph();

    var edgeLevelInfoSet = Set.of(
      new InclinedEdgeLevelInfo(
        new VertexLevelInfo(new Level(1, "1"), 1),
        new VertexLevelInfo(new Level(2, "2"), 2)
      ),
      new InclinedEdgeLevelInfo(new VertexLevelInfo(null, 1), new VertexLevelInfo(null, 2)),
      new InclinedEdgeLevelInfo(
        new VertexLevelInfo(new Level(-1, "P1"), 2),
        new VertexLevelInfo(new Level(1, "1"), 1)
      )
    );
    assertEquals(
      edgeLevelInfoSet,
      getAllInclinedEdgeLevelInfoObjects(graph, streetDetailsRepository)
    );
  }

  @Test
  void testEdgeLevelInfoNotStoredWithoutIncludeEdgeLevelInfo() {
    var inclineStairs = new OsmWay();
    inclineStairs.setId(2);
    inclineStairs.addTag("highway", "steps");
    inclineStairs.addTag("incline", "up");

    var provider = TestOsmProvider.of().addWay(inclineStairs).build();
    var graph = new Graph();
    var streetDetailsRepository = new DefaultStreetDetailsRepository();
    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .withStreetDetailsRepository(streetDetailsRepository)
      .builder()
      // The build config field that needs to bet set for inclined edge level info to be stored.
      .withIncludeInclinedEdgeLevelInfo(false)
      .build()
      .buildGraph();

    assertEquals(Set.of(), getAllInclinedEdgeLevelInfoObjects(graph, streetDetailsRepository));
  }

  @Test
  void testElevatorNodeEdgeLevelInfo() {
    var n1 = node(1, new WgsCoordinate(0, 1));
    var n2 = node(2, new WgsCoordinate(0, 2));
    var elevatorNode = node(3, new WgsCoordinate(0, 3));
    elevatorNode.addTag("highway", "elevator");

    var provider = TestOsmProvider.of()
      .addWayFromNodes(way -> way.addTag("level", "1"), n1, elevatorNode)
      .addWayFromNodes(way -> way.addTag("level", "2"), n2, elevatorNode)
      .build();
    var graph = new Graph();
    var streetDetailsRepository = new DefaultStreetDetailsRepository();
    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .withStreetDetailsRepository(streetDetailsRepository)
      .builder()
      .build()
      .buildGraph();

    var edgeLevelInfoSet = Set.of(new Level(1, "1"), new Level(2, "2"));
    assertEquals(
      edgeLevelInfoSet,
      getAllHorizontalEdgeLevelInfoObjects(graph, streetDetailsRepository)
    );
  }

  @Test
  void testElevatorNodeEdgeLevelInfoOnSameLevel() {
    var n1 = node(1, new WgsCoordinate(0, 1));
    var n2 = node(2, new WgsCoordinate(0, 2));
    var elevatorNode = node(3, new WgsCoordinate(0, 3));
    elevatorNode.addTag("highway", "elevator");

    var provider = TestOsmProvider.of()
      .addWayFromNodes(way -> way.addTag("level", "1"), n1, elevatorNode)
      .addWayFromNodes(way -> way.addTag("level", "1"), n2, elevatorNode)
      .build();
    var graph = new Graph();
    var streetDetailsRepository = new DefaultStreetDetailsRepository();
    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .withStreetDetailsRepository(streetDetailsRepository)
      .builder()
      .build()
      .buildGraph();

    var edgeLevelInfoSet = Set.of(new Level(1, "1"));
    assertEquals(
      edgeLevelInfoSet,
      getAllHorizontalEdgeLevelInfoObjects(graph, streetDetailsRepository)
    );
  }

  @Test
  void testElevatorNodeEdgeLevelInfoWithoutDefinedLevels() {
    var n1 = node(1, new WgsCoordinate(0, 1));
    var n2 = node(2, new WgsCoordinate(0, 2));
    var elevatorNode = node(3, new WgsCoordinate(0, 3));
    elevatorNode.addTag("highway", "elevator");

    var provider = TestOsmProvider.of()
      .addWayFromNodes(n1, elevatorNode)
      .addWayFromNodes(n2, elevatorNode)
      .build();
    var graph = new Graph();
    var streetDetailsRepository = new DefaultStreetDetailsRepository();
    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .withStreetDetailsRepository(streetDetailsRepository)
      .builder()
      .build()
      .buildGraph();

    // The default level should not be stored because it might be misleading to an end user.
    assertEquals(Set.of(), getAllHorizontalEdgeLevelInfoObjects(graph, streetDetailsRepository));
  }

  @Test
  void testElevatorWayEdgeLevelInfo() {
    var elevatorWay = new OsmWay();
    elevatorWay.setId(1);
    elevatorWay.addTag("highway", "elevator");
    elevatorWay.addTag("level", "0;1");

    var provider = TestOsmProvider.of().addWay(elevatorWay).build();
    var graph = new Graph();
    var streetDetailsRepository = new DefaultStreetDetailsRepository();
    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .withStreetDetailsRepository(streetDetailsRepository)
      .builder()
      .build()
      .buildGraph();

    var edgeLevelInfoSet = Set.of(new Level(0, "0"), new Level(1, "1"));
    assertEquals(
      edgeLevelInfoSet,
      getAllHorizontalEdgeLevelInfoObjects(graph, streetDetailsRepository)
    );
  }

  @Test
  void testElevatorWayEdgeLevelInfoOnSameLevel() {
    var elevatorWay = new OsmWay();
    elevatorWay.setId(1);
    elevatorWay.addTag("highway", "elevator");
    elevatorWay.addTag("level", "1;1");

    var provider = TestOsmProvider.of().addWay(elevatorWay).build();
    var graph = new Graph();
    var streetDetailsRepository = new DefaultStreetDetailsRepository();
    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .withStreetDetailsRepository(streetDetailsRepository)
      .builder()
      .build()
      .buildGraph();

    var edgeLevelInfoSet = Set.of(new Level(1, "1"));
    assertEquals(
      edgeLevelInfoSet,
      getAllHorizontalEdgeLevelInfoObjects(graph, streetDetailsRepository)
    );
  }

  @Test
  void testElevatorWayEdgeLevelInfoWithoutDefinedLevels() {
    var elevatorWay = new OsmWay();
    elevatorWay.setId(1);
    elevatorWay.addTag("highway", "elevator");

    var provider = TestOsmProvider.of().addWay(elevatorWay).build();
    var graph = new Graph();
    var streetDetailsRepository = new DefaultStreetDetailsRepository();
    OsmModuleTestFactory.of(provider)
      .withGraph(graph)
      .withStreetDetailsRepository(streetDetailsRepository)
      .builder()
      .build()
      .buildGraph();

    // The default level should not be stored because it might be misleading to an end user.
    assertEquals(Set.of(), getAllHorizontalEdgeLevelInfoObjects(graph, streetDetailsRepository));
  }

  private Set<InclinedEdgeLevelInfo> getAllInclinedEdgeLevelInfoObjects(
    Graph graph,
    StreetDetailsRepository streetDetailsRepository
  ) {
    return graph
      .getEdges()
      .stream()
      .flatMap(edge ->
        streetDetailsRepository
          .findInclinedEdgeLevelInfo(edge)
          .map(Stream::of)
          .orElseGet(Stream::empty)
      )
      .collect(Collectors.toSet());
  }

  private Set<Level> getAllHorizontalEdgeLevelInfoObjects(
    Graph graph,
    StreetDetailsRepository streetDetailsRepository
  ) {
    return graph
      .getEdges()
      .stream()
      .flatMap(edge ->
        streetDetailsRepository
          .findHorizontalEdgeLevelInfo(edge)
          .map(Stream::of)
          .orElseGet(Stream::empty)
      )
      .collect(Collectors.toSet());
  }
}
