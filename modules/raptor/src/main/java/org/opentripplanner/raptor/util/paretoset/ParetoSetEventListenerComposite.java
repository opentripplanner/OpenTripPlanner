package org.opentripplanner.raptor.util.paretoset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * The {@link ParetoSet} do only support ONE listener, this class uses the composite pattern to
 * forward all events to a set of listeners, while playing the role of {@link
 * ParetoSetEventListener} towards the set.
 *
 * @param <T> the set element type
 */
public class ParetoSetEventListenerComposite<T> implements ParetoSetEventListener<T> {

  private final List<ParetoSetEventListener<T>> listeners = new ArrayList<>();

  @SafeVarargs
  public ParetoSetEventListenerComposite(ParetoSetEventListener<T>... listeners) {
    this(Arrays.asList(listeners));
  }

  private ParetoSetEventListenerComposite(
    Collection<? extends ParetoSetEventListener<T>> listeners
  ) {
    this.listeners.addAll(listeners);
  }

  @Override
  public void notifyElementAccepted(T newElement) {
    for (ParetoSetEventListener<T> it : listeners) {
      it.notifyElementAccepted(newElement);
    }
  }

  @Override
  public void notifyElementDropped(T element, T droppedByElement) {
    for (ParetoSetEventListener<T> it : listeners) {
      it.notifyElementDropped(element, droppedByElement);
    }
  }

  @Override
  public void notifyElementRejected(T element, T rejectedByElement) {
    for (ParetoSetEventListener<T> it : listeners) {
      it.notifyElementRejected(element, rejectedByElement);
    }
  }
}
