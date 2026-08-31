package org.opentripplanner.street.graph;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.FreeEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;

class GraphTest {

  @Test
  void testBasic() {
    Graph g = new Graph();
    assertNotNull(g);
  }

  @Test
  void testAddVertex() {
    Vertex a = intersectionVertex("A", 5, 5);
    assertEquals(a.getLabel(), VertexLabel.string("A"));
  }

  @Test
  void testGetVertex() {
    var g = new Graph();
    Vertex a = intersectionVertex("A", 5, 5);
    g.addVertex(a);
    Vertex b = g.getVertex(VertexLabel.string("A"));
    assertEquals(a, b);
  }

  @Test
  void testAddEdge() {
    Vertex a = intersectionVertex("A", 5, 5);
    Vertex b = intersectionVertex("B", 6, 6);
    FreeEdge ee = FreeEdge.createFreeEdge(a, b);
    assertNotNull(ee);
  }

  @Test
  void testListEdgesOneEdge() {
    Graph g = new Graph();
    Vertex a = intersectionVertex("A", 5, 5);
    Vertex b = intersectionVertex("B", 6, 6);

    g.addVertex(a);
    g.addVertex(b);

    var ee = FreeEdge.createFreeEdge(a, b);
    assertThat(g.listEdges()).containsExactlyElementsIn(Set.of(ee));
  }

  @Test
  void testListEdgesMultiple() {
    Graph g = new Graph();
    Vertex a = intersectionVertex("A", 5, 5);
    Vertex b = intersectionVertex("B", 6, 6);
    Vertex c = intersectionVertex("C", 3, 2);

    g.addVertex(a);
    g.addVertex(b);
    g.addVertex(c);

    Set<Edge> expectedEdges = HashSet.newHashSet(4);
    expectedEdges.add(FreeEdge.createFreeEdge(a, b));
    expectedEdges.add(FreeEdge.createFreeEdge(b, c));
    expectedEdges.add(FreeEdge.createFreeEdge(c, b));
    expectedEdges.add(FreeEdge.createFreeEdge(c, a));

    assertThat(g.listEdges()).containsExactlyElementsIn(expectedEdges);
  }

  @Test
  void testGetStreetEdgesNone() {
    Graph g = new Graph();
    Vertex a = intersectionVertex("A", 5, 5);
    Vertex b = intersectionVertex("B", 6, 6);
    Vertex c = intersectionVertex("C", 3, 2);

    Set<Edge> allEdges = HashSet.newHashSet(4);
    allEdges.add(FreeEdge.createFreeEdge(a, b));
    allEdges.add(FreeEdge.createFreeEdge(b, c));
    allEdges.add(FreeEdge.createFreeEdge(c, b));
    allEdges.add(FreeEdge.createFreeEdge(c, a));

    assertThat(g.findEdges(StreetEdge.class)).isEmpty();
  }

  @Test
  void testGetStreetEdgesSeveral() {
    Graph g = new Graph();
    StreetVertex a = intersectionVertex("A", 5, 5);
    StreetVertex b = intersectionVertex("B", 6, 6);
    StreetVertex c = intersectionVertex("C", 3, 2);

    g.addVertex(a);
    g.addVertex(b);
    g.addVertex(c);

    Set<Edge> allStreetEdges = HashSet.newHashSet(4);
    allStreetEdges.add(edge(a, b, 1.0));
    allStreetEdges.add(edge(b, c, 1.0));
    allStreetEdges.add(edge(c, b, 1.0));
    allStreetEdges.add(edge(c, a, 1.0));

    assertThat(g.findEdges(StreetEdge.class)).containsExactlyElementsIn(allStreetEdges);
  }

  @Test
  void iterateEdgesFiltersByType() {
    Graph g = new Graph();
    StreetVertex a = intersectionVertex("A", 5, 5);
    StreetVertex b = intersectionVertex("B", 6, 6);
    StreetVertex c = intersectionVertex("C", 3, 2);

    g.addVertex(a);
    g.addVertex(b);
    g.addVertex(c);

    StreetEdge streetEdge = edge(a, b, 1.0);
    FreeEdge freeEdge = FreeEdge.createFreeEdge(b, c);

    assertThat(g.findEdges(StreetEdge.class)).containsExactly(streetEdge);
    assertThat(g.findEdges(FreeEdge.class)).containsExactly(freeEdge);
    assertThat(g.findEdges(Edge.class)).containsExactly(streetEdge, freeEdge);
  }

  @Test
  void iterateEdgesEmptyGraph() {
    Graph g = new Graph();
    assertThat(g.findEdges(Edge.class)).isEmpty();
  }

  /**
   * Create an edge. If twoWay, create two edges (back and forth).
   */
  private StreetEdge edge(StreetVertex vA, StreetVertex vB, double length) {
    var labelA = vA.getLabel();
    var labelB = vB.getLabel();
    String name = String.format("%s_%s", labelA, labelB);
    Coordinate[] coords = new Coordinate[2];
    coords[0] = vA.getCoordinate();
    coords[1] = vB.getCoordinate();
    LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

    StreetTraversalPermission perm = StreetTraversalPermission.ALL;
    return new StreetEdgeBuilder<>()
      .withFromVertex(vA)
      .withToVertex(vB)
      .withGeometry(geom)
      .withName(name)
      .withMeterLength(length)
      .withPermission(perm)
      .withBack(false)
      .buildAndConnect();
  }
}
