package org.opentripplanner.framework.transaction.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.opentripplanner.framework.event.DomainEvent;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.WriteContext;

/**
 * Task-scoped implementation of {@link WriteContext}.
 *
 * <p>Created fresh for each task submitted to {@link DefaultUpdateManager}. Holds the registered
 * event handler map and gets mutable repositories by casting each
 * {@link RepositoryHandle} to {@link DefaultRepositoryHandle}.
 */
class DefaultWriteContext implements WriteContext {

  private final Map<Class<?>, List<HandlerEntry<?, ?>>> eventHandlers;

  DefaultWriteContext(Map<Class<?>, List<HandlerEntry<?, ?>>> eventHandlers) {
    this.eventHandlers = eventHandlers;
  }

  @Override
  public <M> M repository(RepositoryHandle<?, M> handle) {
    return ((DefaultRepositoryHandle<?, M>) handle).repository();
  }

  @Override
  public void publish(DomainEvent event) {
    List<HandlerEntry<?, ?>> entries = eventHandlers.getOrDefault(
      event.getClass(),
      Collections.emptyList()
    );
    for (var entry : entries) {
      dispatch(entry, event);
    }
  }

  @SuppressWarnings("unchecked")
  private <E extends DomainEvent, M> void dispatch(HandlerEntry<E, M> entry, DomainEvent event) {
    M repository = repository(entry.repoHandle());
    entry.handler().handle((E) event, repository);
  }
}
