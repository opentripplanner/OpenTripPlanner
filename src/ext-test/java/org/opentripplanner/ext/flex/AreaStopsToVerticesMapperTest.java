package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.opentripplanner._support.geometry.Coordinates.BERLIN;
import static org.opentripplanner._support.geometry.Coordinates.HAMBURG;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.BICYCLE_AND_CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_CAR;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

class AreaStopsToVerticesMapperTest {

  private static final TransitModelForTest TEST_MODEL = TransitModelForTest.of();

  private static final AreaStop BERLIN_AREA_STOP = TEST_MODEL.areaStopForTest(
    "berlin",
    Polygons.BERLIN
  );
  public static final StopModel STOP_MODEL = TEST_MODEL
    .stopModelBuilder()
    .withAreaStop(AreaStopsToVerticesMapperTest.BERLIN_AREA_STOP)
    .build();

  public static final TransitModel TRANSIT_MODEL = new TransitModel(STOP_MODEL, new Deduplicator());

  private final List<TestCase> testCases = List.of(
    new TestCase(BERLIN, ALL, Set.of(BERLIN_AREA_STOP)),
    new TestCase(BERLIN, PEDESTRIAN_AND_CAR, Set.of(BERLIN_AREA_STOP)),
    new TestCase(BERLIN, BICYCLE_AND_CAR, Set.of()),
    new TestCase(HAMBURG, ALL, Set.of()),
    new TestCase(BERLIN, PEDESTRIAN, Set.of()),
    new TestCase(HAMBURG, PEDESTRIAN, Set.of()),
    new TestCase(BERLIN, CAR, Set.of())
  );

  @TestFactory
  Stream<DynamicTest> mapAreaStopsInVertex() {
    return testCases
      .stream()
      .map(tc ->
        dynamicTest(
          tc.toString(),
          () -> {
            var graph = new Graph();

            var fromVertex = StreetModelForTest.intersectionVertex(tc.coordinate);
            var toVertex = StreetModelForTest.intersectionVertex(tc.coordinate);

            var edge = StreetModelForTest.streetEdge(fromVertex, toVertex);
            edge.setPermission(tc.permission);
            fromVertex.addOutgoing(edge);

            graph.addVertex(fromVertex);
            assertTrue(fromVertex.areaStops().isEmpty());

            var mapper = new AreaStopsToVerticesMapper(graph, TRANSIT_MODEL);

            mapper.buildGraph();

            assertEquals(tc.expectedAreaStops, fromVertex.areaStops());
          }
        )
      );
  }

  private record TestCase(
    Coordinate coordinate,
    StreetTraversalPermission permission,
    Set<AreaStop> expectedAreaStops
  ) {}
}
