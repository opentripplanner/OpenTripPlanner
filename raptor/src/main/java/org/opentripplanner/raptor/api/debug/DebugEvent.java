package org.opentripplanner.raptor.api.debug;

import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Debug events hold information about an internal event in the Raptor Algorithm. The element may be
 * a stop arrivals, a destination arrival or path.
 *
 * @param <E> the element type.
 */
public class DebugEvent<E> {

  private final Action action;
  private final int iterationStartTime;
  private final E element;
  private final E rejectedDroppedByElement;
  private final String reason;

  /**
   * Private constructor; use static factroy methods to create events.
   */
  private DebugEvent(
    Action action,
    int iterationStartTime,
    E element,
    E rejectedDroppedByElement,
    String reason
  ) {
    this.action = action;
    this.iterationStartTime = iterationStartTime;
    this.element = element;
    this.rejectedDroppedByElement = rejectedDroppedByElement;
    this.reason = reason;
  }

  public static <E> DebugEvent<E> accept(int iterationStartTime, E element) {
    return new DebugEvent<>(Action.ACCEPT, iterationStartTime, element, null, null);
  }

  public static <E> DebugEvent<E> reject(
    int iterationStartTime,
    E element,
    E rejectedByElement,
    String reason
  ) {
    return new DebugEvent<>(Action.REJECT, iterationStartTime, element, rejectedByElement, reason);
  }

  public static <E> DebugEvent<E> drop(
    int iterationStartTime,
    E element,
    E droppedByElement,
    String reason
  ) {
    return new DebugEvent<>(Action.DROP, iterationStartTime, element, droppedByElement, reason);
  }

  /**
   * The acton taken:
   * <ul>
   *     <li>ACCEPT - The element is accepted as one of the best alternatives.
   *     <li>REJECT - The element is rejected, there is a better alternative.
   *     <li>DROP   - The element is dropped from the list of alternatives. Be
   *     aware that that this does not necessarily mean that the path is not part
   *     of the final result. If an element is dropped in a later round or iteration
   *     the original element path might already be added to the final result;
   *     hence dropping the element have no effect on the result.
   * </ul>
   */
  public Action action() {
    return action;
  }

  /**
   * which iteration this event is part of.
   */
  public int iterationStartTime() {
    return iterationStartTime;
  }

  /**
   * The element affected by the action.
   */
  public E element() {
    return element;
  }

  /**
   * The element was dominated  by the this element. This may or may not affect the final result
   * depending on the round/iteration the original element was accepted.
   * <p/>
   * The rejectedDroppedByElement is optional. It can be {@code null}.
   */
  public E rejectedDroppedByElement() {
    return rejectedDroppedByElement;
  }

  /**
   * An element might get rejected or dropped as part of an optimization. The reason should explain
   * why an element is rejected.
   * <p/>
   * The reason is optional, especially if the {@link #rejectedDroppedByElement} is specified.
   *
   * @return If no reason exist an empty string is returned.
   */
  public String reason() {
    return reason == null ? "" : reason;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(DebugEvent.class)
      .addEnum("action", action)
      .addServiceTime("iterationStartTime", iterationStartTime)
      .addObj("element", element)
      .addObj("rejectedDroppedByElement", rejectedDroppedByElement)
      .addStr("reason", reason)
      .toString();
  }

  /** The event action type */
  public enum Action {
    /** Element is accepted */
    ACCEPT("Accept"),
    /** Element is rejected */
    REJECT("Reject"),
    /**
     * Element is dropped from the algorithm state. Since Range Raptor works in rounds and
     * iterations, an element dropped in a later round/iteration might still make it to the optimal
     * solution. This only means that the element is no longer part of the state.
     */
    DROP("Drop");

    private final String description;

    Action(String description) {
      this.description = description;
    }

    @Override
    public String toString() {
      return description;
    }
  }
}
