package org.opentripplanner.framework.snapshot.transaction.internal;

import java.util.function.Supplier;
import org.opentripplanner.framework.snapshot.transaction.RepositoryHandle;
import org.opentripplanner.framework.snapshot.transaction.RepositoryLifecycle;
import org.opentripplanner.framework.snapshot.transaction.RepositoryRegistry;
import org.opentripplanner.framework.snapshot.transaction.RepositoryScope;
import org.opentripplanner.framework.snapshot.transaction.Transaction;
import org.opentripplanner.framework.snapshot.transaction.TransactionalRepository;

/**
 * Default implementation of {@link RepositoryRegistry}.
 *
 * <p>Wraps a {@link InMemoryRepositoryTransactionManager} to coordinate transactions across all
 * registered repositories. Each call to {@link #register(Object, RepositoryLifecycle)} creates a
 * {@link InMemoryTransactionalRepository} internally and returns a {@link RepositoryHandle} typed
 * against the public {@link TransactionalRepository} interface — the concrete implementation class
 * is never exposed to callers.
 */
public class DefaultRepositoryRegistry implements RepositoryRegistry {

  private final InMemoryRepositoryTransactionManager transactionManager =
    new InMemoryRepositoryTransactionManager();

  @Override
  public <S, T> RepositoryHandle<S, T> register(
    S initialSnapshot,
    RepositoryLifecycle<S, T> lifecycle
  ) {
    TransactionalRepository<S, T> repo = new InMemoryTransactionalRepository<>(
      initialSnapshot,
      lifecycle,
      transactionManager
    );
    return new RepositoryHandle<>() {
      @Override
      public S readOnlySnapshot(Transaction transaction) {
        return repo.snapshot(transaction);
      }

      @Override
      public Supplier<T> mutableSnapshot() {
        return repo.mutableSnapshot();
      }
    };
  }

  @Override
  public RepositoryScope scope() {
    return new DefaultRepositoryScope(transactionManager.requestScopedTransaction());
  }

  @Override
  public void commit() {
    transactionManager.commit();
  }
}
