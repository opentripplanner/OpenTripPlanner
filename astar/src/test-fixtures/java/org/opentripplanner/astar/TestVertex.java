package org.opentripplanner.astar;

import java.util.ArrayList;
import java.util.Collection;
import org.opentripplanner.astar.spi.AStarVertex;

public class TestVertex implements AStarVertex<TestState, TestEdge, TestVertex> {

  private final String name;
  private final Collection<TestEdge> incoming = new ArrayList<>();
  private final Collection<TestEdge> outgoing = new ArrayList<>();

  public TestVertex(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }

  @Override
  public Collection<TestEdge> getOutgoing() {
    return outgoing;
  }

  @Override
  public Collection<TestEdge> getIncoming() {
    return incoming;
  }
}
