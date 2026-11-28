package org.opentripplanner.framework.snapshot.transaction;

/**
 * Request-scoped consistent view over one or more repositories.
 *
 * <p>A {@code RepositoryScope} captures the current {@link Transaction} at the moment it is
 * created (typically at the start of a request) and holds a strong reference to it for its
 * lifetime. All calls to {@link #snapshot(RepositoryHandle)} on the same scope instance will
 * resolve against that same transaction, guaranteeing a consistent view across multiple
 * repositories within a single request.
 *
 * <p>The strong reference to the transaction also prevents the corresponding entries in the
 * underlying {@code WeakHashMap} snapshot cache from being garbage-collected while the scope
 * is alive.
 *
 * <p>Obtain a scope via {@link RepositoryRegistry#scope()} at request start. In a Dagger setup
 * this would typically be provided by a request-scoped {@code @Provides} method.
 */
public interface RepositoryScope {
  /**
   * Resolve the read-only snapshot for the given repository handle, using the transaction that
   * was captured when this scope was created.
   *
   * @param handle the application-scoped handle for the repository
   * @param <S>    the read-only snapshot type
   * @return the snapshot as of the transaction captured by this scope
   */
  <S> S snapshot(RepositoryHandle<S, ?> handle);
}
