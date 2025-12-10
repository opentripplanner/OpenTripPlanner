package org.opentripplanner.framework.transaction.internal;

import java.util.EnumMap;
import java.util.Objects;
import java.util.function.Supplier;
import org.opentripplanner.framework.transaction.RepositoryLifecycle;
import org.opentripplanner.framework.transaction.Transaction;
import org.opentripplanner.framework.transaction.TransactionalRepository;

public class DefaultTransactionalRepository<S, T> implements TransactionalRepository<S, T> {

  private final RepositoryLifecycle<S, T> lifecycle;
  private final Supplier<RedBlueTransaction> currentTransactionProvider;
  private final EnumMap<RedBlueTransaction, S> currentSnapshot = new EnumMap<>(
    RedBlueTransaction.class
  );
  private T mutableSnapshot;

  public DefaultTransactionalRepository(
    S currentSnapshot,
    RepositoryLifecycle<S, T> lifecycle,
    DefaultRepositoryTransactionManager manager
  ) {
    this.lifecycle = lifecycle;
    this.currentTransactionProvider = manager.currentTransaction();
    setSnapshot(currentSnapshot, this.currentTransactionProvider.get());
    manager.regester(this);
  }

  @Override
  public S snapshot(Transaction transaction) {
    synchronized (currentSnapshot) {
      return currentSnapshot.get(transaction);
    }
  }

  @Override
  public Supplier<T> mutableSnapshot() {
    return () -> currentMutableSnapshot();
  }

  void commit(RedBlueTransaction currentTransaction, RedBlueTransaction nextTransaction) {
    if (mutableSnapshot != null) {
      setSnapshot(lifecycle.freeze(mutableSnapshot), nextTransaction);
    } else {
      // If there are no modifications, then we will copy over the previous snapshot
      synchronized (currentSnapshot) {
        currentSnapshot.put(nextTransaction, currentSnapshot.get(currentTransaction));
      }
    }
    mutableSnapshot = null;
  }

  private T currentMutableSnapshot() {
    if (mutableSnapshot == null) {
      this.mutableSnapshot = lifecycle.copyOnWrite(snapshot(currentTransactionProvider.get()));
    }
    return mutableSnapshot;
  }

  private void setSnapshot(S snapshot, RedBlueTransaction transaction) {
    Objects.requireNonNull(snapshot);
    synchronized (currentSnapshot) {
      currentSnapshot.put(transaction, snapshot);
    }
  }
}
