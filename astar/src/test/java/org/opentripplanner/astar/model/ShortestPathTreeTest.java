package org.opentripplanner.astar.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.spi.AStarEdge;
import org.opentripplanner.astar.spi.AStarRequest;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.astar.spi.AStarVertex;
import org.opentripplanner.astar.spi.DominanceFunction;

class ShortestPathTreeTest {

  /** Dominance by weight: lower weight dominates. */
  private static final DominanceFunction<TestState> BY_WEIGHT = (a, b) ->
    a.getWeight() <= b.getWeight();

  /** No dominance: all states are co-dominant. */
  private static final DominanceFunction<TestState> NONE = (a, b) -> false;

  @Test
  void singleState() {
    var spt = new ShortestPathTree<>(BY_WEIGHT);
    var v = new TestVertex();
    var s = new TestState(v, 1.0);

    assertTrue(spt.add(s));
    assertEquals(1, spt.getVertexCount());
    assertTrue(spt.visit(s));
    assertEquals(s, spt.getState(v));
    assertEquals(List.of(s), spt.getStates(v));
  }

  @Test
  void dominatedStateReplacement() {
    var spt = new ShortestPathTree<>(BY_WEIGHT);
    var v = new TestVertex();
    var worse = new TestState(v, 10.0);
    var better = new TestState(v, 1.0);

    assertTrue(spt.add(worse));
    assertTrue(spt.add(better));
    // Only better survives — no list promotion
    assertEquals(1, spt.getVertexCount());
    assertFalse(spt.visit(worse));
    assertTrue(spt.visit(better));
    assertEquals(better, spt.getState(v));
    assertEquals(List.of(better), spt.getStates(v));
  }

  @Test
  void dominatedStateRejected() {
    var spt = new ShortestPathTree<>(BY_WEIGHT);
    var v = new TestVertex();
    var better = new TestState(v, 1.0);
    var worse = new TestState(v, 10.0);

    assertTrue(spt.add(better));
    assertFalse(spt.add(worse));
    assertEquals(List.of(better), spt.getStates(v));
  }

  @Test
  void coDominantStates() {
    var spt = new ShortestPathTree<>(NONE);
    var v = new TestVertex();
    var s1 = new TestState(v, 1.0);
    var s2 = new TestState(v, 2.0);

    assertTrue(spt.add(s1));
    assertTrue(spt.add(s2));
    assertEquals(1, spt.getVertexCount());
    assertTrue(spt.visit(s1));
    assertTrue(spt.visit(s2));
    assertEquals(List.of(s1, s2), spt.getStates(v));
  }

  @Test
  void visitReturnsFalseForAbsentVertex() {
    var spt = new ShortestPathTree<>(BY_WEIGHT);
    var v1 = new TestVertex();
    var v2 = new TestVertex();
    var s1 = new TestState(v1, 1.0);
    var s2 = new TestState(v2, 1.0);

    spt.add(s1);
    // s2 was never added
    assertFalse(spt.visit(s2));
  }

  @Test
  void getStatesNullForAbsentVertex() {
    var spt = new ShortestPathTree<>(BY_WEIGHT);
    assertNull(spt.getStates(new TestVertex()));
  }

  @Test
  void getStateNullForAbsentVertex() {
    var spt = new ShortestPathTree<>(BY_WEIGHT);
    assertNull(spt.getState(new TestVertex()));
  }

  @Test
  void getAllStatesAcrossMixedVertices() {
    var spt = new ShortestPathTree<>(NONE);
    var v1 = new TestVertex();
    var v2 = new TestVertex();
    var s1 = new TestState(v1, 1.0);
    var s2a = new TestState(v2, 2.0);
    var s2b = new TestState(v2, 3.0);

    spt.add(s1);
    spt.add(s2a);
    spt.add(s2b);

    Collection<TestState> all = spt.getAllStates();
    assertEquals(3, all.size());
    assertTrue(all.contains(s1));
    assertTrue(all.contains(s2a));
    assertTrue(all.contains(s2b));
  }

  @Test
  void getVertices() {
    var spt = new ShortestPathTree<>(BY_WEIGHT);
    var v1 = new TestVertex();
    var v2 = new TestVertex();
    spt.add(new TestState(v1, 1.0));
    spt.add(new TestState(v2, 1.0));

    var vertices = spt.getVertices();
    assertEquals(2, vertices.size());
    assertTrue(vertices.contains(v1));
    assertTrue(vertices.contains(v2));
  }

  @Test
  void getPathReturnsNullForAbsentVertex() {
    var spt = new ShortestPathTree<>(BY_WEIGHT);
    assertNull(spt.getPath(new TestVertex()));
  }

  @Test
  void getPathReturnsFinalState() {
    var spt = new ShortestPathTree<>(BY_WEIGHT);
    var v = new TestVertex();
    var s = new TestState(v, 1.0);
    spt.add(s);
    assertNotNull(spt.getPath(v));
  }

  @Test
  void toStringShowsVertexCount() {
    var spt = new ShortestPathTree<>(BY_WEIGHT);
    spt.add(new TestState(new TestVertex(), 1.0));
    assertEquals("ShortestPathTree(1 vertices)", spt.toString());
  }

  // -- Minimal test fixtures --

  private static class TestVertex implements AStarVertex<TestState, TestEdge, TestVertex> {

    @Override
    public Collection<TestEdge> getOutgoing() {
      return List.of();
    }

    @Override
    public Collection<TestEdge> getIncoming() {
      return List.of();
    }
  }

  private static class TestState implements AStarState<TestState, TestEdge, TestVertex> {

    private final TestVertex vertex;
    private final double weight;

    TestState(TestVertex vertex, double weight) {
      this.vertex = vertex;
      this.weight = weight;
    }

    @Override
    public boolean isFinal() {
      return true;
    }

    @Override
    public TestState getBackState() {
      return null;
    }

    @Override
    public TestState reverse() {
      return this;
    }

    @Override
    public TestEdge getBackEdge() {
      return null;
    }

    @Override
    public long getTimeSeconds() {
      return 0;
    }

    @Override
    public double getWeight() {
      return weight;
    }

    @Override
    public TestVertex getVertex() {
      return vertex;
    }

    @Override
    public long getElapsedTimeSeconds() {
      return 0;
    }

    @Override
    public Instant getTime() {
      return Instant.EPOCH;
    }

    @Override
    public AStarRequest getRequest() {
      return () -> false;
    }
  }

  private static class TestEdge implements AStarEdge<TestState, TestEdge, TestVertex> {

    @Override
    public TestVertex getFromVertex() {
      return null;
    }

    @Override
    public TestVertex getToVertex() {
      return null;
    }

    @Override
    public TestState[] traverse(TestState s0) {
      return new TestState[0];
    }
  }
}
