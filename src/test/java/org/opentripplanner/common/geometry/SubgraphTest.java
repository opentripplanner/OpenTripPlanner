package org.opentripplanner.common.geometry;

import org.junit.Test;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.BikeParkVertex;
import org.opentripplanner.routing.vertextype.PatternStopVertex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SubgraphTest {

  @Test
  public void testVertex() {
    // Arrange
    final Subgraph subgraph = new Subgraph();
    final BikeParkVertex vertex = new BikeParkVertex(new Graph(),  new BikePark());

    // Act
    subgraph.addVertex(vertex);

    // Assert result
    assertTrue(subgraph.containsStreet(vertex));
    assertEquals(subgraph.streetSize(), 1);
    assertEquals(subgraph.streetIterator().next().toString(), vertex.toString());
    assertEquals(subgraph.getRepresentativeVertex().toString(), vertex.toString());
    assertEquals(subgraph.getConvexHull().getCoordinates().length, 1);
  }

  @Test
  public void testTransitVertex() {
    // Arrange
    final Subgraph subgraph = new Subgraph();
    final PatternStopVertex vertex = new PatternStopVertex(new Graph(), "Test", null, new Stop());

    // Act
    subgraph.addVertex(vertex);

    // Assert result
    assertTrue(subgraph.contains(vertex));
    assertEquals(subgraph.stopSize(), 1);
    assertEquals(subgraph.stopIterator().next().toString(), vertex.toString());
    assertEquals(subgraph.getConvexHull().getCoordinates().length, 1);
  }
}
