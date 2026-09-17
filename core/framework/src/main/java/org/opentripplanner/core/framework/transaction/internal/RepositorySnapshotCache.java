package org.opentripplanner.framework.transaction.internal;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * A thread-safe cache for storing transaction snapshots using weak references to transactions.
 */
class RepositorySnapshotCache<S> {

  private final Map<Transaction, S> cache = new WeakHashMap<>();

  void put(Transaction transaction, S repositorySnapshot) {
    Objects.requireNonNull(repositorySnapshot);
    synchronized (cache) {
      cache.put(transaction, repositorySnapshot);
    }
  }

  S get(Transaction transaction) {
    synchronized (cache) {
      return cache.get(transaction);
    }
  }
}
