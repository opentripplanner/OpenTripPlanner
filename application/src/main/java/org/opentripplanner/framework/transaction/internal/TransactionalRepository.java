package org.opentripplanner.framework.transaction.internal;

import java.util.Objects;
import java.util.function.Supplier;
import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;

class TransactionalRepository<S, M> {

  private final RepositoryLifecycle<S, M> lifecycle;
  private final Supplier<Transaction> transactionProvider;
  private final RepositorySnapshotCache<S> repositorySnapshotCache =
    new RepositorySnapshotCache<>();
  private M repository;

  TransactionalRepository(
    S initialRepositorySnapshot,
    RepositoryLifecycle<S, M> lifecycle,
    TransactionManager manager
  ) {
    this.lifecycle = Objects.requireNonNull(lifecycle);
    this.transactionProvider = manager.currentTransaction();
    this.repositorySnapshotCache.put(this.transactionProvider.get(), initialRepositorySnapshot);
    manager.register(this);
  }

  boolean modified() {
    return repository != null;
  }

  S repositorySnapshot(Transaction transaction) {
    return repositorySnapshotCache.get(transaction);
  }

  Supplier<M> repository() {
    return this::currentRepository;
  }

  void commit(Transaction lastTransaction, Transaction nextTransaction) {
    if (modified()) {
      // Modifications exist, create a new snapshot and put it in the cache.
      S snapshot = lifecycle.freeze(this.repository);
      repositorySnapshotCache.put(nextTransaction, snapshot);
      reset();
    } else {
      // No changes to this repository; carry the last snapshot forward to the next transaction.
      S snapshot = repositorySnapshotCache.get(lastTransaction);
      repositorySnapshotCache.put(nextTransaction, snapshot);
    }
  }

  // Clear the repository reference so a fresh copy-on-write instance is created for the next task.
  void reset() {
    this.repository = null;
  }

  private M currentRepository() {
    if (repository == null) {
      this.repository = lifecycle.copyOnWrite(currentSnapshot());
    }
    return repository;
  }

  private S currentSnapshot() {
    return repositorySnapshot(transactionProvider.get());
  }
}
