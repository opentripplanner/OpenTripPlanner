package org.opentripplanner.raptor.util.paretoset;

import java.util.Objects;

public final class ParetoSetWithListener<T> extends ParetoSet<T> {

  private final ParetoSetEventListener<? super T> eventListener;

  public ParetoSetWithListener(
    ParetoComparator<T> comparator,
    ParetoSetEventListener<? super T> eventListener
  ) {
    super(comparator);
    this.eventListener = Objects.requireNonNull(eventListener);
  }

  @Override
  protected final void notifyElementAccepted(T newElement) {
    eventListener.notifyElementAccepted(newElement);
  }

  @Override
  protected final void notifyElementDropped(T element, T droppedByElement) {
    eventListener.notifyElementDropped(element, droppedByElement);
  }

  @Override
  protected final void notifyElementRejected(T element, T rejectByElement) {
    eventListener.notifyElementRejected(element, rejectByElement);
  }
}
