package org.opentripplanner.framework.transaction.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.opentripplanner.framework.transaction.RepositoryTransactionManager;
import org.opentripplanner.framework.transaction.Transaction;

public class DefaultRepositoryTransactionManager implements RepositoryTransactionManager {

  private final AtomicReference<DefaultTransaction> currentTransaction = new AtomicReference<>(
    DefaultTransaction.next()
  );

  private final List<DefaultTransactionalRepository<?, ?>> repositories = new ArrayList<>();

  @Override
  public Transaction requestScopedTransaction() {
    return currentTransaction.get();
  }

  @Override
  public void commit() {
    var currentTx = currentTransaction.get();
    var nextTx = currentTx.next();

    for (var repository : repositories) {
      repository.commit(currentTx, nextTx);
    }
    currentTransaction.set(nextTx);
  }

  void regester(DefaultTransactionalRepository<?, ?> repository) {
    repositories.add(repository);
  }

  Supplier<DefaultTransaction> currentTransaction() {
    return () -> currentTransaction.get();
  }
}
