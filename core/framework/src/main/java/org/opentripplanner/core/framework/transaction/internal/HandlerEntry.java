package org.opentripplanner.core.framework.transaction.internal;

import org.opentripplanner.core.model.event.DomainEvent;
import org.opentripplanner.core.model.event.EventHandler;
import org.opentripplanner.core.model.transaction.RepositoryHandle;

/**
 * Pairs a {@link EventHandler} with the {@link RepositoryHandle} whose mutable snapshot it writes
 * to.
 */
record HandlerEntry<E extends DomainEvent, M>(
  EventHandler<E, M> handler,
  RepositoryHandle<?, M> repoHandle
) {}
