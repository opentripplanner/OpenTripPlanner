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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
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
    final OSMDatabase osmdb = new OSMDatabase(DataImportIssueStore.NOOP, boardingAreaRefTags);

    final OpenStreetMapModule.Handler handler = new OpenStreetMapModule.Handler(
      graph,
      osmdb,
      DataImportIssueStore.NOOP,
      new OpenStreetMapOptions(
        boardingAreaRefTags,
        null,
        maxAreaNodes,
        false,
        false,
        false,
        false,
        false,
        false
      ),
      new HashMap<>()
    );

    final File file = new File(testInfo.getTestClass().get().getResource(osmFile).getFile());
    new OpenStreetMapProvider(file, true).readOSM(osmdb);
    osmdb.postLoad();

    final WalkableAreaBuilder walkableAreaBuilder = new WalkableAreaBuilder(
      graph,
      osmdb,
      handler,
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
