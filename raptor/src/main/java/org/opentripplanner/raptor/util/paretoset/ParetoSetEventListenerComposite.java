package org.opentripplanner.raptor.util.paretoset;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.util.composite.CompositeUtil;

/**
 * The {@link ParetoSet} do only support ONE listener, this class uses the composite pattern to
 * forward all events to a set of listeners, while playing the role of {@link
 * ParetoSetEventListener} towards the set.
 *
 * @param <T> the set element type
 */
public class ParetoSetEventListenerComposite<T> implements ParetoSetEventListener<T> {

  private final List<ParetoSetEventListener<T>> listeners;

  /**
   * Take a list of listeners and return a composite listener. Input listeners, which are {@code null},
   * are skipped. If no listeners are provided or all listeners are {@code null}, then
   * {@code null} is returned. If just one listener is passed in the listener it-self is returned
   * (without any wrapper). If more than one listener exists, a composite instance is returned.
   */
  @Nullable
  @SafeVarargs
  public static <T> ParetoSetEventListener<T> of(ParetoSetEventListener<T>... listeners) {
    return CompositeUtil.of(
      ParetoSetEventListenerComposite::new,
      it -> it instanceof ParetoSetEventListenerComposite<T>,
      it -> ((ParetoSetEventListenerComposite<T>) it).listeners,
      listeners
    );
  }

  private ParetoSetEventListenerComposite(
    Collection<? extends ParetoSetEventListener<T>> listeners
  ) {
    this.listeners = List.copyOf(listeners);
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

  @Override
  public String toString() {
    return "ParetoSetEventListenerComposite{" + "listeners=" + listeners + '}';
  }
}
