package org.opentripplanner.astar;

import java.util.ArrayList;
import java.util.Collection;
import org.opentripplanner.astar.spi.AStarVertex;

public class TestVertex implements AStarVertex<TestState, TestEdge, TestVertex> {

  private final String name;
  private final ArrayList<TestEdge> incoming = new ArrayList<>();
  private final ArrayList<TestEdge> outgoing = new ArrayList<>();

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
  public TestEdge[] getOutgoingRaw() {
    return outgoing.toArray(new TestEdge[0]);
  }

  @Override
  public Collection<TestEdge> getIncoming() {
    return incoming;
  }

  @Override
  public TestEdge[] getIncomingRaw() {
    return incoming.toArray(new TestEdge[0]);
  }
}
