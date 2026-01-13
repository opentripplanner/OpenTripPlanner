package org.opentripplanner.framework.transaction.internal;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import org.opentripplanner.framework.transaction.RepositoryLifecycle;
import org.opentripplanner.framework.transaction.Transaction;
import org.opentripplanner.framework.transaction.TransactionalRepository;

public class DefaultTransactionalRepository<S, T> implements TransactionalRepository<S, T> {

  private final RepositoryLifecycle<S, T> lifecycle;
  private final Supplier<DefaultTransaction> transactionProvider;
  private final Map<DefaultTransaction, S> snapshotsCashe = new WeakHashMap<>();
  private T mutableSnapshot;

  public DefaultTransactionalRepository(
    S inisialSnapshot,
    RepositoryLifecycle<S, T> lifecycle,
    DefaultRepositoryTransactionManager manager
  ) {
    this.lifecycle = lifecycle;
    this.transactionProvider = manager.currentTransaction();
    setSnapshot(inisialSnapshot, this.transactionProvider.get());
    manager.regester(this);
  }

  @Override
  public S snapshot(Transaction transaction) {
    synchronized (snapshotsCashe) {
      return snapshotsCashe.get(transaction);
    }
  }

  @Override
  public Supplier<T> mutableSnapshot() {
    return () -> currentMutableSnapshot();
  }

  void commit(DefaultTransaction currentTransaction, DefaultTransaction nextTransaction) {
    if (mutableSnapshot != null) {
      setSnapshot(lifecycle.freeze(mutableSnapshot), nextTransaction);
    } else {
      // If there are no modifications, then we will copy over the previous snapshot
      synchronized (snapshotsCashe) {
        snapshotsCashe.put(nextTransaction, snapshotsCashe.get(currentTransaction));
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
    synchronized (snapshotsCashe) {
      snapshotsCashe.put(transaction, snapshot);
    }
  }
}
