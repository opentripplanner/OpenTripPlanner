package org.opentripplanner.framework.snapshot.transaction;

import java.util.function.Supplier;

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
 *   <li><em>Updaters</em> call {@link #mutableSnapshot()} directly to obtain a lazy supplier for
 *       the mutable snapshot. No transaction is involved on the write path.
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

  /**
   * Return a lazy supplier for the mutable snapshot.
   *
   * <p>The supplier performs copy-on-write from the latest committed snapshot on first access.
   * Intended for use by updaters.
   */
  Supplier<T> mutableSnapshot();
}
