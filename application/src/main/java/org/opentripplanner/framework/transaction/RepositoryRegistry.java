package org.opentripplanner.framework.transaction;

import java.util.function.Consumer;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;
import org.opentripplanner.framework.transaction.api.TransactionScope;

/**
 * Application-scoped registry for transactional repositories.
 * <p>
 * This is the entry point for wiring the transaction framework. Typical usage:
 *
 * <ol>
 *   <li>Create one {@code RepositoryRegistry} for the application lifetime.
 *   <li>For each domain repository, call {@link #registerRepositorySnapshot(Object, RepositoryLifecycle)}
 *       during wiring (e.g. in a Dagger module). Keep the returned {@link RepositoryHandle} for
 *       injection into services and updaters.
 *   <li>At the start of each request, call {@link #scope()} to obtain a {@link TransactionScope}
 *       that captures a consistent snapshot of all repositories at that point in time.
 *   <li>To perform writes, use {@link UpdateManager#submit(Consumer)} to add tasks to the manager queue.
 *   <li>Commits are either performed after each submitted task or, if periodic commits are
 *       configured via {@code maxSnapshotFrequency}, in the specified frequency.
 * </ol>
 */
public interface RepositoryRegistry {
  /**
   * Register a new transactional repository and return a typed handle for it.
   * <p>
   * The handle is application-scoped and should be kept for the lifetime of the application,
   * typically by injecting it via Dagger. Do not create and register new repositories after
   * wiring is done, this could lead to concurrency issues.
   *
   * @param initialRepositorySnapshot the initial read-only snapshot for the repository
   * @param lifecycle                 the lifecycle strategy for the given repository/snapshot
   * @param <S>                       the read-only repository snapshot type
   * @param <M>                       the mutable repository type
   * @return an application-scoped handle for accessing the repository and snapshot
   */
  <S, M> RepositoryHandle<S, M> registerRepositorySnapshot(
    S initialRepositorySnapshot,
    RepositoryLifecycle<S, M> lifecycle
  );

  /**
   * This has the same semantics as {@link #registerRepositorySnapshot(Object, RepositoryLifecycle)},
   * but creates the initial snapshot from the repository, using the provided lifecycle. Do not
   * create and register new repositories after wiring is done, this could lead to concurrency
   * issues.
   */
  <S, M> RepositoryHandle<S, M> registerRepository(
    M repository,
    RepositoryLifecycle<S, M> lifecycle
  );

  /**
   * Create a new {@link TransactionScope} capturing the latest commited transaction.
   * <p>
   * All calls to {@link RepositoryHandle#repositorySnapshot(TransactionScope)} on the returned
   * scope will resolve against the same transaction, guaranteeing a consistent read view across
   * all repositories for the duration of the request.
   */
  TransactionScope scope();
}
