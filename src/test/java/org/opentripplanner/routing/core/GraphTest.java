package org.opentripplanner.routing.core;

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
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.FreeEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;

public class GraphTest {

  @Test
  public void testBasic() throws Exception {
    Graph g = new Graph();
    assertNotNull(g);
  }

  @Test
  public void testAddVertex() throws Exception {
    Graph g = new Graph();
    Vertex a = new IntersectionVertex(g, "A", 5, 5);
    assertEquals(a.getLabel(), "A");
  }

  @Test
  public void testGetVertex() throws Exception {
    Graph g = new Graph();
    Vertex a = new IntersectionVertex(g, "A", 5, 5);
    Vertex b = g.getVertex("A");
    assertEquals(a, b);
  }

  @Test
  public void testAddEdge() throws Exception {
    Graph g = new Graph();
    Vertex a = new IntersectionVertex(g, "A", 5, 5);
    Vertex b = new IntersectionVertex(g, "B", 6, 6);
    FreeEdge ee = new FreeEdge(a, b);
    assertNotNull(ee);
  }

  @Test
  public void testGetEdgesOneEdge() {
    Graph g = new Graph();
    Vertex a = new IntersectionVertex(g, "A", 5, 5);
    Vertex b = new IntersectionVertex(g, "B", 6, 6);
    FreeEdge ee = new FreeEdge(a, b);

    List<Edge> edges = new ArrayList<>(g.getEdges());
    assertEquals(1, edges.size());
    assertEquals(ee, edges.get(0));
  }

  @Test
  public void testGetEdgesMultiple() {
    Graph g = new Graph();
    Vertex a = new IntersectionVertex(g, "A", 5, 5);
    Vertex b = new IntersectionVertex(g, "B", 6, 6);
    Vertex c = new IntersectionVertex(g, "C", 3, 2);

    Set<Edge> expectedEdges = new HashSet<>(4);
    expectedEdges.add(new FreeEdge(a, b));
    expectedEdges.add(new FreeEdge(b, c));
    expectedEdges.add(new FreeEdge(c, b));
    expectedEdges.add(new FreeEdge(c, a));

    Set<Edge> edges = new HashSet<>(g.getEdges());
    assertEquals(4, edges.size());
    assertEquals(expectedEdges, edges);
  }

  @Test
  public void testGetStreetEdgesNone() {
    Graph g = new Graph();
    Vertex a = new IntersectionVertex(g, "A", 5, 5);
    Vertex b = new IntersectionVertex(g, "B", 6, 6);
    Vertex c = new IntersectionVertex(g, "C", 3, 2);

    Set<Edge> allEdges = new HashSet<>(4);
    allEdges.add(new FreeEdge(a, b));
    allEdges.add(new FreeEdge(b, c));
    allEdges.add(new FreeEdge(c, b));
    allEdges.add(new FreeEdge(c, a));

    Set<StreetEdge> edges = new HashSet<>(g.getStreetEdges());
    assertEquals(0, edges.size());
  }

  @Test
  public void testGetStreetEdgesSeveral() {
    Graph g = new Graph();
    StreetVertex a = new IntersectionVertex(g, "A", 5, 5);
    StreetVertex b = new IntersectionVertex(g, "B", 6, 6);
    StreetVertex c = new IntersectionVertex(g, "C", 3, 2);

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
  public void testGetEdgesAndVerticesById() {
    Graph g = new Graph();
    StreetVertex a = new IntersectionVertex(g, "A", 5, 5);
    StreetVertex b = new IntersectionVertex(g, "B", 6, 6);
    StreetVertex c = new IntersectionVertex(g, "C", 3, 2);

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
    String labelA = vA.getLabel();
    String labelB = vB.getLabel();
    String name = String.format("%s_%s", labelA, labelB);
    Coordinate[] coords = new Coordinate[2];
    coords[0] = vA.getCoordinate();
    coords[1] = vB.getCoordinate();
    LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

    StreetTraversalPermission perm = StreetTraversalPermission.ALL;
    return new StreetEdge(vA, vB, geom, name, length, perm, false);
  }
}
