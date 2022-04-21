package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporaryVertex;

public class TemporaryVerticesContainerTest {

  private final GeometryFactory gf = GeometryUtils.getGeometryFactory();
  // Given:
  // - a graph with 3 intersections/vertexes
  private final Graph g = new Graph();
  private final StreetVertex a = new IntersectionVertex(g, "A", 1.0, 1.0);
  private final StreetVertex b = new IntersectionVertex(g, "B", 0.0, 1.0);
  private final StreetVertex c = new IntersectionVertex(g, "C", 1.0, 0.0);
  private final List<Vertex> permanentVertexes = Arrays.asList(a, b, c);
  // - And travel *origin* is 0,4 degrees on the road from B to A
  private final GenericLocation from = new GenericLocation(1.0, 0.4);
  // - and *destination* is slightly off 0.7 degrees on road from C to A
  private final GenericLocation to = new GenericLocation(0.701, 1.001);
  private TemporaryVerticesContainer subject;

  // - and some roads
  @BeforeEach
  public void setup() {
    createStreetEdge(a, b, "a -> b");
    createStreetEdge(b, a, "b -> a");
    createStreetEdge(a, c, "a -> c");
    g.index();
  }

  @Test
  public void temporaryChangesRemovedOnClose() {
    // Given - A request
    RoutingRequest request = new RoutingRequest();
    request.from = from;
    request.to = to;

    // When - the container is created
    subject = new TemporaryVerticesContainer(g, request);

    // Then:
    originAndDestinationInsertedCorrect();

    // And When:
    subject.close();

    // Then - permanent vertexes
    for (Vertex v : permanentVertexes) {
      // - does not reference the any temporary nodes anymore
      for (Edge e : v.getIncoming()) {
        assertVertexEdgeIsNotReferencingTemporaryElements(v, e, e.getFromVertex());
      }
      for (Edge e : v.getOutgoing()) {
        assertVertexEdgeIsNotReferencingTemporaryElements(v, e, e.getToVertex());
      }
    }
  }

  private static <T extends Collection<String>> T findAllReachableVertexes(
    Vertex vertex,
    boolean forward,
    T list
  ) {
    if (list.contains(vertex.getDefaultName())) {
      return list;
    }

    list.add(vertex.getDefaultName());
    if (forward) {
      vertex.getOutgoing().forEach(it -> findAllReachableVertexes(it.getToVertex(), forward, list));
    } else {
      vertex
        .getIncoming()
        .forEach(it -> findAllReachableVertexes(it.getFromVertex(), forward, list));
    }
    return list;
  }

  private void originAndDestinationInsertedCorrect() {
    // Then - the origin and destination is
    assertEquals("Origin", subject.getFromVertices().iterator().next().getDefaultName());
    assertEquals("Destination", subject.getToVertices().iterator().next().getDefaultName());

    // And - from the origin
    Collection<String> vertexesReachableFromOrigin = findAllReachableVertexes(
      subject.getFromVertices().iterator().next(),
      true,
      new ArrayList<>()
    );
    String msg = "All reachable vertexes from origin: " + vertexesReachableFromOrigin;

    // it is possible to reach the A, B, C and the Destination Vertex
    assertTrue(vertexesReachableFromOrigin.contains("A"), msg);
    assertTrue(vertexesReachableFromOrigin.contains("B"), msg);
    assertTrue(vertexesReachableFromOrigin.contains("C"), msg);
    assertTrue(vertexesReachableFromOrigin.contains("Destination"), msg);

    // And - from the destination we can backtrack
    Collection<String> vertexesReachableFromDestination = findAllReachableVertexes(
      subject.getToVertices().iterator().next(),
      false,
      new ArrayList<>()
    );
    msg = "All reachable vertexes back from destination: " + vertexesReachableFromDestination;

    // and reach the A, B and the Origin Vertex
    assertTrue(vertexesReachableFromDestination.contains("A"), msg);
    assertTrue(vertexesReachableFromDestination.contains("B"), msg);
    assertTrue(vertexesReachableFromDestination.contains("Origin"), msg);

    // But - not the C Vertex
    assertFalse(vertexesReachableFromDestination.contains("C"), msg);
  }

  private void createStreetEdge(StreetVertex v0, StreetVertex v1, String name) {
    LineString geom = gf.createLineString(
      new Coordinate[] { v0.getCoordinate(), v1.getCoordinate() }
    );
    double dist = SphericalDistanceLibrary.distance(v0.getCoordinate(), v1.getCoordinate());
    new StreetEdge(v0, v1, geom, name, dist, StreetTraversalPermission.ALL, false);
  }

  private void assertVertexEdgeIsNotReferencingTemporaryElements(Vertex src, Edge e, Vertex v) {
    String sourceName = src.getDefaultName();
    assertFalse(e instanceof TemporaryEdge, sourceName + " -> " + e.getDefaultName());
    assertFalse(v instanceof TemporaryVertex, sourceName + " -> " + v.getDefaultName());
  }
}
