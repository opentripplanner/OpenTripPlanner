package org.opentripplanner.raptor.util.paretoset;

import java.util.Objects;

public final class ParetoSetWithListener<T> extends ParetoSet<T> {

  private final ParetoSetEventListener<? super T> eventListener;

  ParetoSetWithListener(
    ParetoComparator<T> comparator,
    ParetoSetEventListener<? super T> eventListener
  ) {
    super(comparator);
    this.eventListener = Objects.requireNonNull(eventListener);
  }

  @Override
  protected void notifyElementAccepted(T newElement) {
    eventListener.notifyElementAccepted(newElement);
  }

  @Override
  protected void notifyElementDropped(T element, T droppedByElement) {
    eventListener.notifyElementDropped(element, droppedByElement);
  }

  @Override
  protected void notifyElementRejected(T element, T rejectByElement) {
    eventListener.notifyElementRejected(element, rejectByElement);
  }
}
