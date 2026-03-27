package org.opentripplanner.street.search.state;

import java.util.Iterator;
import org.opentripplanner.street.model.edge.Edge;

class BackEdgeIterator implements Iterator<Edge> {

  private State current;

  public BackEdgeIterator(State state) {
    current = state;
  }

  @Override
  public boolean hasNext() {
    return current != null && current.getBackState() != null;
  }

  @Override
  public Edge next() {
    var ret = current;
    current = current.getBackState();
    return ret.getBackEdge();
  }
}
