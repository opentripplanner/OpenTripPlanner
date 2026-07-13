package org.opentripplanner.framework.event;

import org.opentripplanner.framework.transaction.api.WriteContext;

/**
 * A write-side event handler that receives a mutable repository snapshot at dispatch time.
 *
 * <p>A {@code RepositoryEventHandler} is invoked inside an active {@link WriteContext}. The
 * context injects the mutable snapshot for the handler's repository as the second argument to
 * {@link #handle(DomainEvent, Object)}, so the handler never holds a stored reference to anything
 * mutable.
 *
 * @param <E> the domain event type this handler responds to
 * @param <M> the mutable snapshot type this handler writes to
 */
public interface EventHandler<E extends DomainEvent, M> {
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
