package org.opentripplanner.core.framework.transaction.internal;

import org.opentripplanner.core.domain.framework.event.DomainEvent;
import org.opentripplanner.core.domain.framework.event.EventHandler;
import org.opentripplanner.core.domain.framework.transaction.api.RepositoryHandle;

/**
 * Pairs a {@link EventHandler} with the {@link RepositoryHandle} whose mutable snapshot it writes
 * to.
 */
record HandlerEntry<E extends DomainEvent, M>(
  EventHandler<E, M> handler,
  RepositoryHandle<?, M> repoHandle
) {}
