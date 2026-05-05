package org.opentripplanner.framework.snapshot.transaction.internal;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import org.opentripplanner.framework.snapshot.transaction.RepositoryLifecycle;
import org.opentripplanner.framework.snapshot.transaction.Transaction;
import org.opentripplanner.framework.snapshot.transaction.TransactionalRepository;

class InMemoryTransactionalRepository<S, T> implements TransactionalRepository<S, T> {

  private final RepositoryLifecycle<S, T> lifecycle;
  private final Supplier<DefaultTransaction> transactionProvider;
  private final Map<Transaction, S> snapshotCash = new WeakHashMap<>();
  private T mutableSnapshot;

  InMemoryTransactionalRepository(
    S initialSnapshot,
    RepositoryLifecycle<S, T> lifecycle,
    InMemoryRepositoryTransactionManager manager
  ) {
    this.lifecycle = lifecycle;
    this.transactionProvider = manager.currentTransaction();
    setSnapshot(initialSnapshot, this.transactionProvider.get());
    manager.register(this);
  }

  @Override
  public S snapshot(Transaction transaction) {
    synchronized (snapshotCash) {
      return snapshotCash.get(transaction);
    }
  }

  Supplier<T> mutableSnapshot() {
    return this::currentMutableSnapshot;
  }

  void commit(DefaultTransaction currentTransaction, DefaultTransaction nextTransaction) {
    if (mutableSnapshot != null) {
      setSnapshot(lifecycle.freeze(mutableSnapshot), nextTransaction);
    } else {
      // If there are no modifications, then we will copy over the previous snapshot
      synchronized (snapshotCash) {
        snapshotCash.put(nextTransaction, snapshotCash.get(currentTransaction));
      }
    }
    mutableSnapshot = null;
  }

  private T currentMutableSnapshot() {
    if (mutableSnapshot == null) {
      this.mutableSnapshot = lifecycle.copyOnWrite(snapshot(transactionProvider.get()));
    }
    return mutableSnapshot;
  }

  private void setSnapshot(S snapshot, DefaultTransaction transaction) {
    Objects.requireNonNull(snapshot);
    synchronized (snapshotCash) {
      snapshotCash.put(transaction, snapshot);
    }
  }
}
