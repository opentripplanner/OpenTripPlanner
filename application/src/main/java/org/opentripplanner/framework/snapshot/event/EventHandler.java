package org.opentripplanner.framework.snapshot.event;

import org.opentripplanner.framework.snapshot.transaction.WriteContext;

/**
 * A write-side event handler that receives a mutable repository snapshot at dispatch time.
 *
 * A {@code RepositoryEventHandler} is invoked
 * inside an active {@link WriteContext}. The context
 * injects the mutable snapshot for the handler's repository as the second argument to
 * {@link #handle(DomainEvent, Object)}, so the handler never holds a stored reference to anything
 * mutable.
 *
 * <p>Implementations must be one of the permitted per-repository subtypes:
 * {@link EventHandlerTimetable} or {@link EventHandlerTransfer}.
 *
 * @param <E> the domain event type this handler responds to
 * @param <M> the mutable snapshot type this handler writes to
 */
public sealed interface EventHandler<E extends DomainEvent, M>
  permits EventHandlerTimetable, EventHandlerTransfer {
  /**
   * The domain event type this handler is interested in.
   */
  Class<E> eventType();

  /**
   * Handle the event, writing to the provided mutable snapshot.
   *
   * @param event           the domain event
   * @param mutableSnapshot the mutable snapshot for this handler's repository, injected by the
   *                        {@link WriteContext}
   */
  void handle(E event, M mutableSnapshot);
}
