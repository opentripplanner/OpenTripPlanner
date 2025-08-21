package org.opentripplanner.graph_builder.module.osm;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMultimap;
import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.naming.DefaultNamer;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.street.model.vertex.VertexLabel.OsmNodeOnLevelLabel;
import org.opentripplanner.test.support.ResourceLoader;

public class WalkableAreaBuilderTest {

  private DefaultOsmInfoGraphBuildRepository osmInfoRepository;

  public Graph buildGraph(final TestInfo testInfo) {
    var graph = new Graph();
    final Method testMethod = testInfo.getTestMethod().get();
    final String osmFile = testMethod.getAnnotation(OsmFile.class).value();
    final boolean visibility = testMethod.getAnnotation(Visibility.class).value();
    final int maxAreaNodes = testMethod.getAnnotation(MaxAreaNodes.class).value();
    final boolean platformEntriesLinking = true;

    final Set<String> boardingAreaRefTags = Set.of("ref");
    final OsmDatabase osmdb = new OsmDatabase(DataImportIssueStore.NOOP);
    this.osmInfoRepository = new DefaultOsmInfoGraphBuildRepository();

    final File file = ResourceLoader.of(WalkableAreaBuilderTest.class).file(osmFile);
    assertTrue(file.exists());
    new DefaultOsmProvider(file, false).readOsm(osmdb);
    osmdb.postLoad();

    final WalkableAreaBuilder walkableAreaBuilder = new WalkableAreaBuilder(
      graph,
      osmdb,
      osmInfoRepository,
      new VertexGenerator(osmdb, graph, Set.of(), false, DataImportIssueStore.NOOP),
      new DefaultNamer(),
      new SafetyValueNormalizer(graph, DataImportIssueStore.NOOP),
      DataImportIssueStore.NOOP,
      maxAreaNodes,
      platformEntriesLinking,
      boardingAreaRefTags
    );

    final Map<OsmArea, OsmLevel> areasLevels = osmdb
      .getWalkableAreas()
      .stream()
      .collect(toMap(a -> a, a -> osmdb.getLevelForWay(a.parent)));
    final List<OsmAreaGroup> areaGroups = OsmAreaGroup.groupAreas(
      areasLevels,
      ImmutableMultimap.of()
    );

    final Consumer<OsmAreaGroup> build = visibility
      ? walkableAreaBuilder::buildWithVisibility
      : walkableAreaBuilder::buildWithoutVisibility;

    areaGroups.forEach(build);
    return graph;
  }

  // -- Tests --

  @Test
  @OsmFile("lund-station-sweden.osm.pbf")
  @Visibility(true)
  @MaxAreaNodes(5)
  void testCalculateVerticesArea(TestInfo testInfo) {
    var graph = buildGraph(testInfo);
    var areas = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> a.getToVertex().getLabel().equals(VertexLabel.osm(1025307935)))
      .map(AreaEdge::getArea)
      .distinct()
      .toList();
    assertEquals(1, areas.size());
    assertFalse(areas.get(0).getAreas().isEmpty());
  }

  @Test
  @OsmFile("lund-station-sweden.osm.pbf")
  @Visibility(false)
  @MaxAreaNodes(5)
  void testSetupCalculateVerticesAreaWithoutVisibility(TestInfo testInfo) {
    var graph = buildGraph(testInfo);
    var areas = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> a.getToVertex().getLabel().equals(VertexLabel.osm(1025307935)))
      .map(AreaEdge::getArea)
      .distinct()
      .toList();
    assertEquals(1, areas.size());
    assertFalse(areas.get(0).getAreas().isEmpty());
  }

  // test that entrance node in a stop area relation does not link across different levels and layers
  // test also that entrance linking of stop area with multiple platforms works properly
  @Test
  @OsmFile("stopareas.pbf")
  @Visibility(true)
  @MaxAreaNodes(50)
  void testEntranceStopAreaLinking(TestInfo testInfo) {
    var graph = buildGraph(testInfo);
    // first platform contains isolated node tagged as highway=bus_stop. Those are linked if level matches.
    var busStopConnection = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> a.getToVertex().getLabel().equals(VertexLabel.osm(143853)))
      .map(AreaEdge::getArea)
      .distinct()
      .toList();
    assertEquals(1, busStopConnection.size());

    // first platform has level 0, entrance below it has level -1 -> no links
    var entranceAtWrongLevel = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> a.getToVertex().getLabel().equals(VertexLabel.osm(143850)))
      .map(AreaEdge::getArea)
      .distinct()
      .toList();
    assertEquals(0, entranceAtWrongLevel.size());

    // second platform and its entrance both default to level zero, entrance gets connected
    var entranceAtSameLevel = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> hasNodeId(a, 143832))
      .map(AreaEdge::getArea)
      .distinct()
      .toList();
    assertEquals(1, entranceAtSameLevel.size());

    // second platform also contains a stop position which is not considered as an entrance
    // therefore it should not get linked
    var stopPositionConnection = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> a.getToVertex().getLabel().equals(VertexLabel.osm(143863)))
      .map(AreaEdge::getArea)
      .distinct()
      .toList();
    assertEquals(0, stopPositionConnection.size());

    // test that third platform and its entrance get connected
    // and there are not too many connections (to remote platforms)
    // third platform also tests the 'layer' tag
    var connectionEdges = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> hasNodeId(a, 143845))
      .toList();
    // entrance is connected top 2 opposite corners of a single platform
    // with two bidirectional edge pairs, and with the other entrance point
    assertEquals(3, connectionEdges.size());

    // test that semicolon separated list of elevator levals works in level matching
    // e.g. 'level'='0;1'
    var elevatorConnection = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> hasNodeId(a, 143861))
      .map(AreaEdge::getArea)
      .distinct()
      .toList();
    assertEquals(1, elevatorConnection.size());

    // first platform area has ref tag. Check that it is available in
    // DefaultOsmInfoGraphBuildRepository
    var areaGroups = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> a.getToVertex().getLabel().equals(VertexLabel.osm(143846)))
      .map(AreaEdge::getArea)
      .distinct()
      .toList();

    var area = areaGroups.getFirst().getAreas().getFirst();
    var platform = this.osmInfoRepository.findPlatform(area);
    assertTrue(platform.isPresent());
    assertEquals(Set.of("007"), platform.get().references());
    // test that boarding location for a concave platform is inside the platform area
    assertTrue(area.getGeometry().intersects(platform.get().geometry()));
  }

  @Test
  @OsmFile("wendlingen-bahnhof.osm.pbf")
  @Visibility(true)
  @MaxAreaNodes(50)
  void testSeveralIntersections(TestInfo testInfo) {
    var graph = buildGraph(testInfo);
    var areas = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> a.getToVertex().getLabel().equals(VertexLabel.osm(2522105666L)))
      .map(AreaEdge::getArea)
      .distinct()
      .toList();
    assertEquals(1, areas.size());
    assertFalse(areas.get(0).getAreas().isEmpty());
  }

  private static boolean hasNodeId(AreaEdge a, long nodeId) {
    return (
      a.getToVertex().getLabel() instanceof OsmNodeOnLevelLabel label && label.nodeId() == nodeId
    );
  }

  // -- Infrastructure --

  @Retention(RUNTIME)
  @Target(ElementType.METHOD)
  public @interface OsmFile {
    String value();
  }

  @Retention(RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Visibility {
    boolean value();
  }

  @Retention(RUNTIME)
  @Target(ElementType.METHOD)
  public @interface MaxAreaNodes {
    int value();
  }
}
