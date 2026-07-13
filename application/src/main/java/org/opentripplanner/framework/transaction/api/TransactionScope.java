package org.opentripplanner.framework.transaction.api;

import org.opentripplanner.framework.transaction.RepositoryRegistry;

/**
 * Request-scoped consistent view over one or more repositories.
 * <p>
 * A {@code TransactionScope} captures the current transaction at the moment it is created
 * (typically at the start of a request) and holds a strong reference to it for its lifetime.
 * All {@link RepositoryHandle#repositorySnapshot(TransactionScope)} calls on the same scope
 * instance resolve against that same transaction, guaranteeing a consistent read view across
 * multiple repositories within a single request.
 * <p>
 * The strong reference to the transaction also prevents the corresponding entries in the
 * underlying {@code WeakHashMap} repository snapshot cache from being garbage-collected while the
 * scope is alive.
 * <p>
 * Get a scope via {@link RepositoryRegistry#scope()} at request start. In a Dagger setup
 * this would typically be provided by a request-scoped {@code @Provides} method.
 */
public interface TransactionScope {}
