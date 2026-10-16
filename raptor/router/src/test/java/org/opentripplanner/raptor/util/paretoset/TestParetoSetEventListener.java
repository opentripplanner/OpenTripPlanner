package org.opentripplanner.raptor.util.paretoset;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An event listener which keeps the result for each event type. Used in tests in this package
 * only!
 */
class TestParetoSetEventListener<T> implements ParetoSetEventListener<T> {

  private final List<T> accepted = new ArrayList<>();
  private final List<T> rejected = new ArrayList<>();
  private final List<T> dropped = new ArrayList<>();

  @Override
  public void notifyElementAccepted(T newElement) {
    accepted.add(newElement);
  }

  @Override
  public void notifyElementDropped(T element, T droppedByElement) {
    dropped.add(element);
  }

  @Override
  public void notifyElementRejected(T element, T rejectedByElement) {
    rejected.add(element);
  }

  @Override
  public String toString() {
    return "TestParetoSetEventListener";
  }

  public String acceptedAsString() {
    return toString(accepted);
  }

  public String rejectedAsString() {
    return toString(rejected);
  }

  public String droppedAsString() {
    return toString(dropped);
  }

  void clear() {
    accepted.clear();
    rejected.clear();
    dropped.clear();
  }

  private String toString(List<T> list) {
    return list.stream().map(Object::toString).collect(Collectors.joining(" "));
  }
}
