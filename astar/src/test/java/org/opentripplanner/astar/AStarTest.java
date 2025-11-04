package org.opentripplanner.astar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AStarTest {

  @Test
  void simple() {
    var vA = vertex("A");
    var vB = vertex("B");
    var vC = vertex("C");

    edges(vA, vB, 10);
    edges(vB, vC, 10);

    var tree = new TestAStarBuilder().withFrom(vA).withTo(vC).getShortestPathTree();

    var path = tree.getPath(vC);

    var states = path.states;

    assertEquals(1, states.size());
    assertEquals(20, path.getWeight());
    assertEquals(vC, states.getFirst().getVertex());
  }

  @Test
  void twoOptions() {
    var vA = vertex("A");
    var vB1 = vertex("B1");
    var vB2 = vertex("B2");
    var vC = vertex("C");

    edges(vA, vB1, 10);
    edges(vA, vB2, 20);
    edges(vB1, vC, 11);
    edges(vB2, vC, 11);

    var tree = new TestAStarBuilder().withFrom(vA).withTo(vC).getShortestPathTree();

    var path = tree.getPath(vC);

    var states = path.states;

    assertEquals(1, states.size());
    assertEquals(21, path.getWeight());
    assertEquals(vC, states.getFirst().getVertex());
  }

  @Test
  void moreEdgesButLowerCost() {
    var from = vertex("A");
    var vB1 = vertex("B1");
    var vB2 = vertex("B2");
    var vC = vertex("C");
    var to = vertex("D");

    edges(from, vB1, 10);
    edges(from, vB2, 5);
    edges(vB1, to, 20);
    edges(vB2, vC, 5);
    edges(vC, to, 5);

    var tree = new TestAStarBuilder().withFrom(from).withTo(to).getShortestPathTree();

    var path = tree.getPath(to);

    var states = path.states;

    assertEquals(1, states.size());
    assertEquals(15, path.getWeight());
    assertEquals(to, states.getFirst().getVertex());
  }

  private TestVertex vertex(String label) {
    return new TestVertex(label);
  }

  private void edges(TestVertex from, TestVertex to, double weight) {
    new TestEdge(from, to, weight);
    new TestEdge(to, from, weight);
  }
}
