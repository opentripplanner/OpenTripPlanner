package org.opentripplanner.raptor.rangeraptor.debug;

import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.DebugHandler;
import org.opentripplanner.raptor.util.paretoset.ParetoSetEventListener;

/**
 * Attach a path debug handler to a pareto set of elements that are not paths, for example
 * destination arrivals. The pareto set works on the light-weight elements, and the path is only
 * built when an event is reported. Hence, this has no cost when debugging is off, and the debug
 * API can still report {@link RaptorPath}s.
 *
 * @param <E> The pareto set element type.
 */
final class ParetoSetDebugPathAdapter<E> implements ParetoSetEventListener<E> {

  private final DebugHandler<RaptorPath<?>> debugHandler;
  private final Function<E, RaptorPath<?>> toPath;

  /**
   * @param toPath Map an element to a path. The function may return {@code null} if the element
   *               should not be reported - this is useful for elements which are not part of the
   *               search, like a probe used for qualification checks.
   */
  ParetoSetDebugPathAdapter(
    DebugHandler<RaptorPath<?>> debugHandler,
    Function<E, RaptorPath<?>> toPath
  ) {
    this.debugHandler = debugHandler;
    this.toPath = toPath;
  }

  @Override
  public void notifyElementAccepted(E newElement) {
    var path = toPath.apply(newElement);
    if (path != null) {
      debugHandler.accept(path);
    }
  }

  @Override
  public void notifyElementDropped(E element, E droppedByElement) {
    var path = toPath.apply(element);
    if (path != null) {
      debugHandler.drop(path, toPathOrNull(droppedByElement), null);
    }
  }

  @Override
  public void notifyElementRejected(E element, E rejectedByElement) {
    var path = toPath.apply(element);
    if (path != null) {
      debugHandler.reject(path, toPathOrNull(rejectedByElement), null);
    }
  }

  @Nullable
  private RaptorPath<?> toPathOrNull(@Nullable E element) {
    return element == null ? null : toPath.apply(element);
  }
}
