package org.opentripplanner.astar;

import org.opentripplanner.astar.spi.AStarEdge;

public class TestEdge implements AStarEdge<TestState, TestEdge, TestVertex> {

  private final TestVertex from;
  private final TestVertex to;
  private final double weight;

  TestEdge(TestVertex from, TestVertex to, double weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
    to.getIncoming().add(this);
    from.getOutgoing().add(this);
  }

  @Override
  public TestVertex getFromVertex() {
    return from;
  }

  @Override
  public TestVertex getToVertex() {
    return to;
  }

  @Override
  public TestState[] traverse(TestState s0) {
    return new TestState[] { new TestState(to, s0.getWeight() + weight) };
  }
}
