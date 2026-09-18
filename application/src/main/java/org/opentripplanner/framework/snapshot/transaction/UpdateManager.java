package org.opentripplanner.framework.snapshot.transaction;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.opentripplanner.framework.snapshot.event.DomainEvent;
import org.opentripplanner.framework.snapshot.event.EventHandler;

/**
 * Application-scoped manager for write operations against transactional repositories.
 *
 * <p>{@code UpdateManager} is the single public entry point for all writes. It owns a
 * single-threaded executor that serialises tasks, preventing concurrent mutation of mutable
 * snapshots. After each submitted task completes, the manager commits all pending changes
 * atomically — {@code commit()} is never exposed publicly.
 *
 * <p>Typical usage:
 * <ol>
 *   <li>At startup, register {@link EventHandler}s via
 *       {@link #register(EventHandler, RepositoryHandle)}.
 *   <li>When an updater has work to do, call {@link #submit(Consumer)} with a lambda that
 *       receives a fresh {@link WriteContext}. Construct the updater service inside the lambda.
 *   <li>The {@code WriteContext} provides mutable snapshot access and event publication.
 *       Commit happens automatically after the lambda returns.
 * </ol>
 */
public interface UpdateManager {
  /**
   * Register an event handler at startup.
   *
   * <p>When a {@link org.opentripplanner.framework.snapshot.event.DomainEvent} matching
   * {@code handler.eventType()} is published via {@link WriteContext#publish}, the
   * {@link WriteContext} will call {@code handler.handle(event, mutable(repoHandle))},
   * injecting the mutable snapshot for {@code repoHandle} at dispatch time.
   *
   * @param handler    the event handler to register
   * @param repoHandle the repository handle whose mutable snapshot the handler writes to
   * @param <E>        the domain event type
   * @param <M>        the mutable snapshot type
   */
  <E extends DomainEvent, M> void register(
    EventHandler<E, M> handler,
    RepositoryHandle<?, M> repoHandle
  );

  /**
   * Submit an update task for execution on the single writer thread.
   *
   * <p>The task receives a fresh {@link WriteContext} scoped to this invocation. All writes and
   * event publications must go through the context. After the task returns, all pending mutable
   * snapshots are committed atomically.
   *
   * @param task the update task to execute
   * @return a {@link Future} that completes after the task has run and changes have been committed
   */
  Future<Void> submit(Consumer<WriteContext> task);
}
