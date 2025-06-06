package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner._support.geometry.Coordinates.BERLIN;
import static org.opentripplanner._support.geometry.Coordinates.HAMBURG;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.BICYCLE_AND_CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_CAR;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

class AreaStopsToVerticesMapperTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final AreaStop BERLIN_AREA_STOP = TEST_MODEL.areaStop("berlin")
    .withGeometry(Polygons.BERLIN)
    .build();
  public static final SiteRepository SITE_REPOSITORY = TEST_MODEL.siteRepositoryBuilder()
    .withAreaStop(AreaStopsToVerticesMapperTest.BERLIN_AREA_STOP)
    .build();

  public static final TimetableRepository TRANSIT_MODEL = new TimetableRepository(
    SITE_REPOSITORY,
    new Deduplicator()
  );

  static List<TestCase> testCases() {
    return List.of(
      new TestCase(BERLIN, ALL, Set.of(BERLIN_AREA_STOP)),
      new TestCase(BERLIN, PEDESTRIAN_AND_CAR, Set.of(BERLIN_AREA_STOP)),
      new TestCase(BERLIN, BICYCLE_AND_CAR, Set.of()),
      new TestCase(HAMBURG, ALL, Set.of()),
      new TestCase(BERLIN, PEDESTRIAN, Set.of()),
      new TestCase(HAMBURG, PEDESTRIAN, Set.of()),
      new TestCase(BERLIN, CAR, Set.of())
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void mapAreaStopsInVertex(TestCase tc) {
    var graph = new Graph();

    var fromVertex = StreetModelForTest.intersectionVertex(tc.coordinate);
    var toVertex = StreetModelForTest.intersectionVertex(tc.coordinate);

    var edge = StreetModelForTest.streetEdge(fromVertex, toVertex);
    edge.setPermission(tc.permission);
    fromVertex.addOutgoing(edge);

    graph.addVertex(fromVertex);
    assertTrue(fromVertex.areaStops().isEmpty());
    graph.index();

    var mapper = new AreaStopsToVerticesMapper(graph, TRANSIT_MODEL);

    mapper.buildGraph();

    assertEquals(tc.expectedAreaStops, fromVertex.areaStops());
  }

  private record TestCase(
    Coordinate coordinate,
    StreetTraversalPermission permission,
    Set<AreaStop> expectedAreaStops
  ) {}
}
