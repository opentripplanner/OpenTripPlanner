package org.opentripplanner.framework.transaction.internal;

import org.opentripplanner.framework.event.DomainEvent;
import org.opentripplanner.framework.event.EventHandler;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;

/**
 * Pairs a {@link EventHandler} with the {@link RepositoryHandle} whose mutable snapshot it writes
 * to.
 */
record HandlerEntry<E extends DomainEvent, M>(
  EventHandler<E, M> handler,
  RepositoryHandle<?, M> repoHandle
) {}
