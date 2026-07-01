package org.opentripplanner.framework.transaction.internal;

import java.util.Objects;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.TransactionScope;

/**
 * Default request-scoped implementation of {@link TransactionScope}.
 * <p>
 * Captures the current {@link Transaction} at construction time and holds a strong reference to it for its lifetime.
 * This prevents the corresponding repository snapshot cache entries in the underlying {@code WeakHashMap} from being garbage-collected while a request is active, and guarantees that
 * all {@link RepositoryHandle#repositorySnapshot(TransactionScope)} calls passing this scope resolve against
 * the same transaction.
 */
@SuppressWarnings("ClassCanBeRecord")
class DefaultTransactionScope implements TransactionScope {

  private final Transaction transaction;

  DefaultTransactionScope(Transaction transaction) {
    this.transaction = Objects.requireNonNull(transaction);
  }

  Transaction transaction() {
    return transaction;
  }

  @Override
  public String toString() {
    return "Scope(" + transaction + ')';
  }
}
