package org.opentripplanner.raptor.rangeraptor.internalapi;

/**
 * This interface serve as a debug handler for the Worker and State classes. They ues this interface
 * to report stop arrival events, pattern ride events and destination arrival events.
 * <p/>
 * The implementation of this interface will take these events and report them back to the API
 * listeners, passed in as part of the debug request.
 *
 * @param <T> The element type reported to the handler
 */
public interface DebugHandler<T> {
  /**
   * Retuns TRUE if a listener exist and there the given stop index is in the stops or path list.
   */
  boolean isDebug(int stop);

  /**
   * Callback to notify that the given element is accepted into the given collection. For example
   * this happens when a new stop arrival is accepted at a particular stop.
   * <p/>
   * The handler will do the last check to see if this stop is in the request stop list or in debug
   * request path.
   */
  void accept(T element);

  /**
   * Callback to notify that the given element is rejected by the given collection.
   * <p/>
   * The same check as in {@link #accept(Object)} is performed before reporting back to the API
   * listeners.
   *
   * @param element           the rejected element
   * @param rejectedByElement the dominating element. Optional can be {@code null}
   */
  void reject(T element, T rejectedByElement, String reason);

  /**
   * Callback to notify that the given element is dropped, because a new and even more shiny element
   * is found.
   * <p/>
   * The same check as in {@link #accept(Object)} is performed before reporting back to the API
   * listeners.
   */
  void drop(T element, T droppedByElement, String reason);
}
