package org.opentripplanner.raptor.util.paretoset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * The {@link ParetoSet} do only support ONE listener, this class uses the composite pattern to
 * forward all events to a set of listeners, while playing the role of {@link
 * ParetoSetEventListener} towards the set.
 *
 * @param <T> the set element type
 */
public class ParetoSetEventListenerComposite<T> implements ParetoSetEventListener<T> {

  private final List<ParetoSetEventListener<T>> listeners = new ArrayList<>();

  /**
   * Take a list of listeners and return a composite listener. Input listeners witch is {@code null}
   * is skipped. If no listeners are provided, all listeners are {@code null}, then
   * {@code null} is returned. If just one listener is passed in the listener it-self is returned
   * (without any wrapper).
   */
  @Nullable
  @SafeVarargs
  public static <T> ParetoSetEventListener<T> of(ParetoSetEventListener<T>... listeners) {
    var list = Arrays.stream(listeners).filter(Objects::nonNull).toList();
    if (list.isEmpty()) {
      return null;
    }
    if (list.size() == 1) {
      return list.get(0);
    }
    return new ParetoSetEventListenerComposite<>(list);
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
