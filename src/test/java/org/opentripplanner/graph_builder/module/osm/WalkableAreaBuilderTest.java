package org.opentripplanner.graph_builder.module.osm;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.naming.DefaultNamer;
import org.opentripplanner.openstreetmap.OsmProvider;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class WalkableAreaBuilderTest {

  private final Deduplicator deduplicator = new Deduplicator();
  private final Graph graph = new Graph(deduplicator);

  @BeforeEach
  public void testSetup(final TestInfo testInfo) {
    final Method testMethod = testInfo.getTestMethod().get();
    final String osmFile = testMethod.getAnnotation(OsmFile.class).value();
    final boolean visibility = testMethod.getAnnotation(Visibility.class).value();
    final boolean platformEntriesLinking = true;
    final int maxAreaNodes = 5;

    final Set<String> boardingAreaRefTags = Set.of();
    final OsmDatabase osmdb = new OsmDatabase(DataImportIssueStore.NOOP, boardingAreaRefTags);

    final File file = new File(testInfo.getTestClass().get().getResource(osmFile).getFile());
    new OsmProvider(file, true).readOSM(osmdb);
    osmdb.postLoad();

    final WalkableAreaBuilder walkableAreaBuilder = new WalkableAreaBuilder(
      graph,
      osmdb,
      new VertexGenerator(osmdb, graph, Set.of()),
      new DefaultNamer(),
      new SafetyValueNormalizer(graph, DataImportIssueStore.NOOP),
      DataImportIssueStore.NOOP,
      maxAreaNodes,
      platformEntriesLinking,
      boardingAreaRefTags
    );

    final Map<Area, OSMLevel> areasLevels = osmdb
      .getWalkableAreas()
      .stream()
      .collect(toMap(a -> a, a -> osmdb.getLevelForWay(a.parent)));
    final List<AreaGroup> areaGroups = AreaGroup.groupAreas(areasLevels);

    final Consumer<AreaGroup> build = visibility
      ? walkableAreaBuilder::buildWithVisibility
      : walkableAreaBuilder::buildWithoutVisibility;

    areaGroups.forEach(build);
  }

  // -- Tests --

  @Test
  @OsmFile("lund-station-sweden.osm.pbf")
  @Visibility(true)
  public void test_calculate_vertices_area() {
    var areas = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> a.getToVertex().getLabel().equals("osm:node:1025307935"))
      .map(AreaEdge::getArea)
      .distinct()
      .toList();
    assertEquals(1, areas.size());
    assertFalse(areas.get(0).getAreas().isEmpty());
  }

  @Test
  @OsmFile("lund-station-sweden.osm.pbf")
  @Visibility(false)
  public void testSetup_calculate_vertices_area_without_visibility() {
    var areas = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> a.getToVertex().getLabel().equals("osm:node:1025307935"))
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
  public void test_entrance_stoparea_linking() {
    // first platform has level 0, entrance below it has level -1 -> no links
    var entranceAtWrongLevel = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> a.getToVertex().getLabel().startsWith("osm:node:-143850"))
      .map(AreaEdge::getArea)
      .distinct()
      .toList();
    assertEquals(0, entranceAtWrongLevel.size());

    // second platform and its entrance both default to level zero, entrance gets connected
    var entranceAtSameLevel = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> a.getToVertex().getLabel().startsWith("osm:node:-143832"))
      .map(AreaEdge::getArea)
      .distinct()
      .toList();
    assertEquals(1, entranceAtSameLevel.size());

    // second platform also contains a stop position which is not considered as an entrance
    // therefore it should not get linked
    var stopPositionConnection = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> a.getToVertex().getLabel().startsWith("osm:node:-143863"))
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
      .filter(a -> a.getToVertex().getLabel().startsWith("osm:node:-143845"))
      .toList();
    // entrance is connected top 2 opposite corners of a single platform
    // with two bidirectional edge pairs, and with the other entrance point
    assertEquals(6, connectionEdges.size());

    // test that semicolon separated list of elevator levals works in level matching
    // e.g. 'level'='0;1'
    var elevatorConnection = graph
      .getEdgesOfType(AreaEdge.class)
      .stream()
      .filter(a -> a.getToVertex().getLabel().startsWith("osm:node:-143861"))
      .map(AreaEdge::getArea)
      .distinct()
      .toList();
    assertEquals(1, elevatorConnection.size());
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
}
