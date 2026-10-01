package org.opentripplanner.street.search.state;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator for traversing a state chain backwards without any extra allocations.
 */
class BackStateIterator implements Iterator<State> {

  private State current;

  public BackStateIterator(State state) {
    current = state;
  }

  @Override
  public boolean hasNext() {
    return current != null;
  }

  @Override
  public State next() {
    if (!hasNext()) {
      throw new NoSuchElementException("No more back states available");
    }
    var ret = current;
    current = current.getBackState();
    return ret;
  }
}
