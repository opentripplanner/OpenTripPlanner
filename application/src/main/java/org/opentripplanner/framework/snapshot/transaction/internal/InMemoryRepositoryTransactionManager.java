package org.opentripplanner.framework.snapshot.transaction.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.opentripplanner.framework.snapshot.transaction.Transaction;

public class InMemoryRepositoryTransactionManager {

  private final AtomicReference<DefaultTransaction> currentTransaction = new AtomicReference<>(
    DefaultTransaction.next()
  );

  private final List<InMemoryTransactionalRepository<?, ?>> repositories = new ArrayList<>();

  public Transaction requestScopedTransaction() {
    return currentTransaction.get();
  }

  public void commit() {
    var currentTx = currentTransaction.get();
    var nextTx = DefaultTransaction.next();

    for (var repository : repositories) {
      repository.commit(currentTx, nextTx);
    }
    currentTransaction.set(nextTx);
  }

  void register(InMemoryTransactionalRepository<?, ?> repository) {
    repositories.add(repository);
  }

  Supplier<DefaultTransaction> currentTransaction() {
    return currentTransaction::get;
  }
}
