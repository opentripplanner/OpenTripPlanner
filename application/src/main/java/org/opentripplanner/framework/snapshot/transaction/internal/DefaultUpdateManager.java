package org.opentripplanner.framework.snapshot.transaction.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.opentripplanner.framework.snapshot.event.DomainEvent;
import org.opentripplanner.framework.snapshot.event.EventHandler;
import org.opentripplanner.framework.snapshot.transaction.RepositoryHandle;
import org.opentripplanner.framework.snapshot.transaction.UpdateManager;
import org.opentripplanner.framework.snapshot.transaction.WriteContext;

/**
 * Default implementation of {@link UpdateManager}.
 *
 * <p>Owns a single-threaded {@link ExecutorService} that serialises all submitted tasks.
 * After each task completes, changes are committed via the
 * {@link InMemoryRepositoryTransactionManager}. The transaction manager is package-private and
 * never exposed to callers — commit is an internal implementation detail.
 */
class DefaultUpdateManager implements UpdateManager {

  private final InMemoryRepositoryTransactionManager transactionManager;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Map<Class<?>, List<DefaultWriteContext.HandlerEntry<?, ?>>> eventHandlers =
    new HashMap<>();

  public DefaultUpdateManager(InMemoryRepositoryTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  @Override
  public <E extends DomainEvent, M> void register(
    EventHandler<E, M> handler,
    RepositoryHandle<?, M> repoHandle
  ) {
    eventHandlers
      .computeIfAbsent(handler.eventType(), k -> new ArrayList<>())
      .add(new DefaultWriteContext.HandlerEntry<>(handler, repoHandle));
  }

  @Override
  public Future<Void> submit(Consumer<WriteContext> task) {
    return executor.submit(() -> {
      WriteContext ctx = new DefaultWriteContext(eventHandlers);
      task.accept(ctx);
      transactionManager.commit();
      return null;
    });
  }
}
