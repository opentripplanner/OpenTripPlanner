package org.opentripplanner.framework.snapshot.transaction;

/**
 * Application-scoped registry for transactional repositories.
 *
 * <p>This is the entry point for wiring the transaction framework. Typical usage:
 *
 * <ol>
 *   <li>Create one {@code RepositoryRegistry} for the application lifetime.
 *   <li>For each domain repository, call {@link #register(Object, RepositoryLifecycle)} during
 *       wiring (e.g. in a Dagger module). Keep the returned {@link RepositoryHandle} for
 *       injection into services and updaters.
 *   <li>At the start of each request, call {@link #scope()} to obtain a {@link RepositoryScope}
 *       that captures a consistent snapshot of all repositories at that point in time.
 *   <li>To perform writes, use the
 *       {@link UpdateManager}, which commits
 *       changes automatically after each submitted task.
 * </ol>
 */
public interface RepositoryRegistry {
  /**
   * Register a new transactional repository and return a typed handle for it.
   *
   * <p>The handle is application-scoped and should be kept for the lifetime of the application,
   * typically by injecting it via Dagger.
   *
   * @param initialSnapshot the initial read-only snapshot for the repository
   * @param lifecycle       the copy-on-write / freeze strategy for this repository's snapshot types
   * @param <S>             the read-only snapshot type
   * @param <T>             the mutable snapshot type
   * @return an application-scoped handle for accessing this repository
   */
  <S, T> RepositoryHandle<S, T> register(S initialSnapshot, RepositoryLifecycle<S, T> lifecycle);

  /**
   * Create a new {@link RepositoryScope} capturing the current transaction.
   *
   * <p>All calls to {@link RepositoryScope#snapshot(RepositoryHandle)} on the returned scope will
   * resolve against the same transaction, guaranteeing a consistent read view across all
   * repositories for the duration of the request.
   *
   * <p>In a Dagger setup this method would be called from a request-scoped {@code @Provides}
   * method.
   */
  RepositoryScope scope();
}
