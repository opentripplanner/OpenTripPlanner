package org.opentripplanner.framework.transaction.api;

import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.UpdateManager;

/**
 * Application-scoped handle for a transactional repository. Obtained once at wiring time via
 * {@link RepositoryRegistry#registerRepositorySnapshot(Object, RepositoryLifecycle)} and then injected
 * wherever repository access is needed.
 *
 * <ul>
 *   <li>Request-scoped <em>services</em> receive a {@link TransactionScope} from the
 *       {@link RepositoryRegistry} and call {@link #repositorySnapshot(TransactionScope)}, which guarantees
 *       a consistent read view across all repositories for the duration of the request.
 *   <li><em>Updaters</em> obtain write access exclusively through a {@link WriteContext} provided
 *       by the {@link UpdateManager}. Handles are read-only from the public API.
 * </ul>
 *
 * The {@code M} type parameter is not used by the public API, but allows the framework to return
 * a type-safe handle to callers.
 *
 * @param <S> the read-only repository snapshot type
 * @param <M> the mutable repository type
 */
@SuppressWarnings("unused")
public interface RepositoryHandle<S, M> {
  /**
   * Resolve the repository snapshot for the given scope, using the transaction that was captured
   * when this scope was created.
   *
   * @return the repository snapshot as of the transaction captured by this scope
   */
  S repositorySnapshot(TransactionScope scope);
}
