package org.opentripplanner.framework.snapshot.transaction.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.opentripplanner.framework.snapshot.event.DomainEvent;
import org.opentripplanner.framework.snapshot.event.EventHandler;
import org.opentripplanner.framework.snapshot.transaction.RepositoryHandle;
import org.opentripplanner.framework.snapshot.transaction.WriteContext;

/**
 * Task-scoped implementation of {@link WriteContext}.
 *
 * <p>Created fresh for each task submitted to {@link DefaultUpdateManager}. Holds the registered
 * event handler map and delegates mutable snapshot access to the internal {@link WritableHandle}
 * on each repository handle.
 */
class DefaultWriteContext implements WriteContext {

  private final Map<Class<?>, List<HandlerEntry<?, ?>>> eventHandlers;

  DefaultWriteContext(Map<Class<?>, List<HandlerEntry<?, ?>>> eventHandlers) {
    this.eventHandlers = eventHandlers;
  }

  @Override
  public <M> Supplier<M> mutable(RepositoryHandle<?, M> handle) {
    return ((WritableHandle<M>) handle).mutableSnapshot();
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

  private <E extends DomainEvent, M> void dispatch(HandlerEntry<E, M> entry, DomainEvent event) {
    M mutableSnapshot = mutable(entry.repoHandle()).get();
    entry.handler().handle((E) event, mutableSnapshot);
  }

  /**
   * Pairs a {@link EventHandler} with the {@link RepositoryHandle} whose mutable
   * snapshot it writes to.
   */
  record HandlerEntry<E extends DomainEvent, M>(
    EventHandler<E, M> handler,
    RepositoryHandle<?, M> repoHandle
  ) {}
}
