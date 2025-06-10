package org.opentripplanner.routing.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
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
    Vertex a = StreetModelForTest.intersectionVertex("A", 5, 5);
    assertEquals(a.getLabel(), VertexLabel.string("A"));
  }

  @Test
  void testGetVertex() {
    var g = new Graph();
    Vertex a = StreetModelForTest.intersectionVertex("A", 5, 5);
    g.addVertex(a);
    Vertex b = g.getVertex("A");
    assertEquals(a, b);
  }

  @Test
  void testAddEdge() {
    Vertex a = StreetModelForTest.intersectionVertex("A", 5, 5);
    Vertex b = StreetModelForTest.intersectionVertex("B", 6, 6);
    FreeEdge ee = FreeEdge.createFreeEdge(a, b);
    assertNotNull(ee);
  }

  @Test
  void testGetEdgesOneEdge() {
    Graph g = new Graph();
    Vertex a = StreetModelForTest.intersectionVertex("A", 5, 5);
    Vertex b = StreetModelForTest.intersectionVertex("B", 6, 6);

    g.addVertex(a);
    g.addVertex(b);

    FreeEdge ee = FreeEdge.createFreeEdge(a, b);

    List<Edge> edges = new ArrayList<>(g.getEdges());
    assertEquals(1, edges.size());
    assertEquals(ee, edges.get(0));
  }

  @Test
  void testGetEdgesMultiple() {
    Graph g = new Graph();
    Vertex a = StreetModelForTest.intersectionVertex("A", 5, 5);
    Vertex b = StreetModelForTest.intersectionVertex("B", 6, 6);
    Vertex c = StreetModelForTest.intersectionVertex("C", 3, 2);

    g.addVertex(a);
    g.addVertex(b);
    g.addVertex(c);

    Set<Edge> expectedEdges = new HashSet<>(4);
    expectedEdges.add(FreeEdge.createFreeEdge(a, b));
    expectedEdges.add(FreeEdge.createFreeEdge(b, c));
    expectedEdges.add(FreeEdge.createFreeEdge(c, b));
    expectedEdges.add(FreeEdge.createFreeEdge(c, a));

    Set<Edge> edges = new HashSet<>(g.getEdges());
    assertEquals(4, edges.size());
    assertEquals(expectedEdges, edges);
  }

  @Test
  void testGetStreetEdgesNone() {
    Graph g = new Graph();
    Vertex a = StreetModelForTest.intersectionVertex("A", 5, 5);
    Vertex b = StreetModelForTest.intersectionVertex("B", 6, 6);
    Vertex c = StreetModelForTest.intersectionVertex("C", 3, 2);

    Set<Edge> allEdges = new HashSet<>(4);
    allEdges.add(FreeEdge.createFreeEdge(a, b));
    allEdges.add(FreeEdge.createFreeEdge(b, c));
    allEdges.add(FreeEdge.createFreeEdge(c, b));
    allEdges.add(FreeEdge.createFreeEdge(c, a));

    Set<StreetEdge> edges = new HashSet<>(g.getStreetEdges());
    assertEquals(0, edges.size());
  }

  @Test
  void testGetStreetEdgesSeveral() {
    Graph g = new Graph();
    StreetVertex a = StreetModelForTest.intersectionVertex("A", 5, 5);
    StreetVertex b = StreetModelForTest.intersectionVertex("B", 6, 6);
    StreetVertex c = StreetModelForTest.intersectionVertex("C", 3, 2);

    g.addVertex(a);
    g.addVertex(b);
    g.addVertex(c);

    Set<Edge> allStreetEdges = new HashSet<>(4);
    allStreetEdges.add(edge(a, b, 1.0));
    allStreetEdges.add(edge(b, c, 1.0));
    allStreetEdges.add(edge(c, b, 1.0));
    allStreetEdges.add(edge(c, a, 1.0));

    Set<StreetEdge> edges = new HashSet<>(g.getStreetEdges());
    assertEquals(4, edges.size());
    assertEquals(allStreetEdges, edges);
  }

  @Test
  void testGetEdgesAndVerticesById() {
    StreetVertex a = StreetModelForTest.intersectionVertex("A", 5, 5);
    StreetVertex b = StreetModelForTest.intersectionVertex("B", 6, 6);
    StreetVertex c = StreetModelForTest.intersectionVertex("C", 3, 2);

    Set<Edge> allEdges = new HashSet<>(4);
    allEdges.add(edge(a, b, 1.0));
    allEdges.add(edge(b, c, 1.0));
    allEdges.add(edge(c, b, 1.0));
    allEdges.add(edge(c, a, 1.0));
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
