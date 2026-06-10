package org.opentripplanner.street.search.state;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.opentripplanner.street.model.edge.Edge;

/**
 * Iterator for traversing back edges in a state chain without any extra allocations.
 */
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
    if (!hasNext()) {
      throw new NoSuchElementException("No more back edges available");
    }
    var ret = current;
    current = current.getBackState();
    return ret.getBackEdge();
  }
}
