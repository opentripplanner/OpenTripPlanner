package org.opentripplanner.framework.transaction.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.framework.event.DomainEvent;
import org.opentripplanner.framework.event.EventHandler;
import org.opentripplanner.framework.transaction.UpdateManager;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.WriteContext;

/**
 * Default implementation of {@link UpdateManager}.
 *
 * <p>Owns a single-threaded {@link ExecutorService} that serialises all submitted tasks.
 * After each task completes, changes are committed via the
 * {@link TransactionManager}. The transaction manager is package-private and
 * never exposed to callers — commit is an internal implementation detail.
 */
class DefaultUpdateManager implements UpdateManager {

  private final TransactionManager transactionManager;
  private final ExecutorService executor;
  private final Map<Class<?>, List<HandlerEntry<?, ?>>> eventHandlers = new HashMap<>();
  private final PeriodicCommitScheduler periodicCommitScheduler;

  DefaultUpdateManager(
    String name,
    TransactionManager transactionManager,
    ThreadFactory threadFactory,
    @Nullable Duration commitInterval
  ) {
    this.transactionManager = transactionManager;
    this.executor = Executors.newSingleThreadExecutor(threadFactory);
    this.periodicCommitScheduler = commitInterval != null && !commitInterval.isZero()
      ? new PeriodicCommitScheduler(name, commitInterval, threadFactory, this::submitCommit)
      : null;
  }

  @Override
  public <E extends DomainEvent, M> void register(
    EventHandler<E, M> handler,
    RepositoryHandle<?, M> repoHandle
  ) {
    eventHandlers
      .computeIfAbsent(handler.eventType(), k -> new ArrayList<>())
      .add(new HandlerEntry<>(handler, repoHandle));
  }

  @Override
  public Future<Void> submit(Consumer<WriteContext> task) {
    return useAtomicCommit() ? submitAndAutoCommit(task) : submitAndReturn(task);
  }

  @Override
  public void shutdown() {
    if (periodicCommitScheduler != null) {
      periodicCommitScheduler.shutdown();
    }
    executor.shutdown();
  }

  private boolean useAtomicCommit() {
    return periodicCommitScheduler == null;
  }

  private Future<Void> submitAndAutoCommit(Consumer<WriteContext> task) {
    return executor.submit(() -> {
      try {
        task.accept(new DefaultWriteContext(eventHandlers));
        transactionManager.commit();
      } catch (RuntimeException e) {
        rollback();
        throw e;
      }
      return null;
    });
  }

  private Future<Void> submitAndReturn(Consumer<WriteContext> task) {
    return executor.submit(() -> {
      task.accept(new DefaultWriteContext(eventHandlers));
      return null;
    });
  }

  private Future<Void> submitCommit() {
    return executor.submit(() -> {
      transactionManager.commit();
      return null;
    });
  }

  private void rollback() {
    transactionManager.rollback();
  }
}
