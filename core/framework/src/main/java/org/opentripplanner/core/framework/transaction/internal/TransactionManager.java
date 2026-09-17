package org.opentripplanner.framework.transaction.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

class TransactionManager {

  private final AtomicLong idSequence = new AtomicLong(0);

  private final AtomicReference<Transaction> currentTransaction = new AtomicReference<>(next());

  private final List<TransactionalRepository<?, ?>> repositories = new ArrayList<>();

  public Transaction requestScopedTransaction() {
    return currentTransaction.get();
  }

  public void commit() {
    // Skip commit if no modification exist
    if (repositories.stream().noneMatch(TransactionalRepository::modified)) {
      return;
    }
    var currentTx = currentTransaction.get();
    var nextTx = next();

    for (var repository : repositories) {
      repository.commit(currentTx, nextTx);
    }
    currentTransaction.set(nextTx);
  }

  void rollback() {
    for (var repository : repositories) {
      repository.reset();
    }
  }

  void register(TransactionalRepository<?, ?> repository) {
    repositories.add(repository);
  }

  Supplier<Transaction> currentTransaction() {
    return currentTransaction::get;
  }

  private Transaction next() {
    return new Transaction(idSequence.incrementAndGet());
  }
}
