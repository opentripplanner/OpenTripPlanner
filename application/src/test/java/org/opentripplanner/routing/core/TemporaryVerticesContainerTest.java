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
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.module.linking.TestVertexLinker;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.TemporaryEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class TemporaryVerticesContainerTest {

  // Given:
  // - a graph with 3 intersections/vertexes
  private final Graph g = new Graph(new Deduplicator());

  private final StreetVertex a = StreetModelForTest.intersectionVertex("A", 1.0, 1.0);
  private final StreetVertex b = StreetModelForTest.intersectionVertex("B", 1.0, 0.0);
  private final StreetVertex c = StreetModelForTest.intersectionVertex("C", 0.0, 1.0);
  private final List<Vertex> permanentVertexes = Arrays.asList(a, b, c);
  // - And travel *origin* is 0,4 degrees on the road from B to A
  private final GenericLocation from = GenericLocation.fromCoordinate(1.0, 0.4);
  // - and *destination* is slightly off 0.7 degrees on road from C to A
  private final GenericLocation to = GenericLocation.fromCoordinate(0.701, 1.001);
  private TemporaryVerticesContainer subject;

  // - and some roads
  @BeforeEach
  public void setup() {
    permanentVertexes.forEach(g::addVertex);
    createStreetEdge(a, b, "a -> b");
    createStreetEdge(b, a, "b -> a");
    createStreetEdge(a, c, "a -> c");
    g.index();
  }

  @Test
  public void temporaryChangesRemovedOnClose() {
    // When - the container is created
    subject = new TemporaryVerticesContainer(
      g,
      TestVertexLinker.of(g),
      id -> List.of(),
      from,
      to,
      StreetMode.WALK,
      StreetMode.WALK
    );

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
    double dist = SphericalDistanceLibrary.distance(v0.getCoordinate(), v1.getCoordinate());
    StreetModelForTest.streetEdgeBuilder(v0, v1, dist, StreetTraversalPermission.ALL)
      .withName(I18NString.of(name))
      .buildAndConnect();
  }

  private void assertVertexEdgeIsNotReferencingTemporaryElements(Vertex src, Edge e, Vertex v) {
    String sourceName = src.getDefaultName();
    assertFalse(e instanceof TemporaryEdge, sourceName + " -> " + e.getDefaultName());
    assertFalse(v instanceof TemporaryVertex, sourceName + " -> " + v.getDefaultName());
  }
}
