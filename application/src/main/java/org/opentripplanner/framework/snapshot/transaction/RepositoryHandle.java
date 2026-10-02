package org.opentripplanner.framework.snapshot.transaction;

/**
 * Application-scoped typed access point for a single {@link TransactionalRepository}.
 *
 * <p>A handle is obtained once at wiring time via
 * {@link RepositoryRegistry#register(Object, RepositoryLifecycle)} and then injected wherever
 * repository access is needed.
 *
 * <ul>
 *   <li>Request-scoped <em>services</em> should not call this directly. Instead they receive a
 *       {@link RepositoryScope} from the framework and call
 *       {@link RepositoryScope#snapshot(RepositoryHandle)} on it, which guarantees that all
 *       repositories in one request are resolved against the same transaction.
 *   <li><em>Updaters</em> obtain write access exclusively through a
 *       {@link WriteContext} provided by the
 *       {@link UpdateManager}. Handles are
 *       read-only from the public API.
 * </ul>
 *
 * @param <S> the read-only snapshot type
 * @param <T> the mutable snapshot type
 */
public interface RepositoryHandle<S, T> {
  /**
   * Resolve a read-only snapshot for the given transaction.
   *
   * <p>This is an internal method called by {@link RepositoryScope}. Application code should use
   * the scope instead of calling this directly.
   */
  S readOnlySnapshot(Transaction transaction);
}
