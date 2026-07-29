package org.opentripplanner.graph_builder.module.osm;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelForTest.osmVertex;
import static org.opentripplanner.street.model.StreetModelForTest.streetEdge;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;

import java.util.List;
import org.geotools.geometry.jts.JTS;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner._support.geometry.Coordinates;

class PlatformEntranceFinderTest {

  private static final Coordinate C1 = Coordinates.of(0, 0);
  private static final Coordinate C2 = Coordinates.of(0.0001, 0.0001);
  private static final Coordinate C3 = Coordinates.of(0.0002, 0.0002);
  private static final double DEFAULT_BUFFER = 0.001;

  @Test
  void findsStubEndpointsWithinPolygon() {
    var v1 = osmVertex(C1, 1);
    var v2 = osmVertex(C2, 2);
    var v3 = osmVertex(C3, 3);
    streetEdge(v1, v2, PEDESTRIAN);
    streetEdge(v2, v1, PEDESTRIAN);
    streetEdge(v2, v3, PEDESTRIAN);
    streetEdge(v3, v2, PEDESTRIAN);

    var index = PlatformEntranceFinder.of(List.of(v1, v2, v3));

    assertThat(index.findPlatformVerticesWithin(polygonAround(C1, 0.00005))).containsExactly(v1);
  }

  @Test
  void excludesMultiEntryHubVertex() {
    var v1 = osmVertex(C1, 1);
    var v2 = osmVertex(C2, 2);
    var v3 = osmVertex(C3, 3);
    streetEdge(v1, v2, PEDESTRIAN);
    streetEdge(v2, v1, PEDESTRIAN);
    streetEdge(v2, v3, PEDESTRIAN);
    streetEdge(v3, v2, PEDESTRIAN);

    var index = PlatformEntranceFinder.of(List.of(v1, v2, v3));

    // a polygon spanning the whole chain finds both dead-end stubs, but never the hub v2,
    // which has two distinct ways in and is therefore not a single-entry candidate
    assertThat(index.findPlatformVerticesWithin(polygonAround(C2))).containsExactly(v1, v3);
  }

  @Test
  void returnsEmptyOutsideQueryPolygon() {
    var v1 = osmVertex(C1, 1);
    var v2 = osmVertex(C2, 2);
    streetEdge(v1, v2, PEDESTRIAN);
    streetEdge(v2, v1, PEDESTRIAN);

    var index = PlatformEntranceFinder.of(List.of(v1, v2));

    assertThat(index.findPlatformVerticesWithin(polygonAround(Coordinates.HAMBURG))).isEmpty();
  }

  @Test
  void excludesVertexWithoutEdges() {
    var isolated = osmVertex(Coordinates.HAMBURG, 1);

    var index = PlatformEntranceFinder.of(List.of(isolated));

    assertThat(index.findPlatformVerticesWithin(polygonAround(Coordinates.HAMBURG))).isEmpty();
  }

  @Test
  void excludesNonOsmVertices() {
    var v1 = intersectionVertex(C1);
    var v2 = intersectionVertex(C2);
    streetEdge(v1, v2, PEDESTRIAN);
    streetEdge(v2, v1, PEDESTRIAN);

    var index = PlatformEntranceFinder.of(List.of(v1, v2));

    assertThat(index.findPlatformVerticesWithin(polygonAround(C1))).isEmpty();
  }

  @Test
  void excludesMotorizedStub() {
    var v1 = osmVertex(C1, 1);
    var v2 = osmVertex(C2, 2);
    streetEdge(v1, v2, CAR);
    streetEdge(v2, v1, CAR);

    var index = PlatformEntranceFinder.of(List.of(v1, v2));

    assertThat(index.findPlatformVerticesWithin(polygonAround(C1))).isEmpty();
  }

  @Test
  void emptyIndexHasNoCandidates() {
    var index = PlatformEntranceFinder.empty();

    assertThat(index.findPlatformVerticesWithin(polygonAround(C1, 1))).isEmpty();
  }

  private static Polygon polygonAround(Coordinate c) {
    return polygonAround(c, DEFAULT_BUFFER);
  }

  private static Polygon polygonAround(Coordinate c, double buffer) {
    var env = new Envelope();
    env.expandToInclude(c);
    env.expandBy(buffer);
    return JTS.toGeometry(env);
  }
}
