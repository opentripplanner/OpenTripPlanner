package org.opentripplanner.framework.snapshot.event;

import org.opentripplanner.framework.snapshot.domain.transfer.repository.MutableTransferSnapshot;

/**
 * A {@link EventHandler} that writes to the transfer repository.
 *
 * @param <E> the domain event type
 */
public non-sealed interface EventHandlerTransfer<E extends DomainEvent>
  extends EventHandler<E, MutableTransferSnapshot> {}
