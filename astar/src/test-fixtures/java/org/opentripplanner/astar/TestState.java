package org.opentripplanner.astar;

import java.time.Instant;
import org.opentripplanner.astar.spi.AStarRequest;
import org.opentripplanner.astar.spi.AStarState;

class TestState implements AStarState<TestState, TestEdge, TestVertex> {

  private final TestVertex vertex;
  private final double weight;

  public TestState(TestVertex vertex, double weight) {
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
    return null;
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
    return null;
  }

  @Override
  public void initBackEdge(TestEdge originBackEdge) {}

  @Override
  public AStarRequest getRequest() {
    return () -> false;
  }
}
