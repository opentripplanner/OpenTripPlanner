package org.opentripplanner.framework.snapshot.transaction.internal;


import org.opentripplanner.framework.snapshot.transaction.RepositoryHandle;
import org.opentripplanner.framework.snapshot.transaction.RepositoryScope;
import org.opentripplanner.framework.snapshot.transaction.Transaction;

/**
 * Default request-scoped implementation of {@link RepositoryScope}.
 *
 * <p>Captures the current {@link Transaction} at construction time and holds a strong reference
 * to it for its lifetime. This prevents the corresponding snapshot cache entries in the underlying
 * {@code WeakHashMap} from being garbage-collected while a request is active, and guarantees that
 * all {@link #snapshot(RepositoryHandle)} calls within the same scope resolve against the same
 * transaction.
 */
public class DefaultRepositoryScope implements RepositoryScope {

  private final Transaction transaction;

  DefaultRepositoryScope(Transaction transaction) {
    this.transaction = transaction;
  }

  @Override
  public <S> S snapshot(RepositoryHandle<S, ?> handle) {
    return handle.readOnlySnapshot(transaction);
  }
}
