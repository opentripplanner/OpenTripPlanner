package org.opentripplanner.framework.snapshot.transaction.internal;

import java.util.function.Supplier;
import org.opentripplanner.framework.snapshot.transaction.RepositoryHandle;
import org.opentripplanner.framework.snapshot.transaction.RepositoryLifecycle;
import org.opentripplanner.framework.snapshot.transaction.RepositoryRegistry;
import org.opentripplanner.framework.snapshot.transaction.RepositoryScope;
import org.opentripplanner.framework.snapshot.transaction.Transaction;
import org.opentripplanner.framework.snapshot.transaction.TransactionalRepository;
import org.opentripplanner.framework.snapshot.transaction.UpdateManager;

/**
 * Default implementation of {@link RepositoryRegistry}.
 *
 * <p>Wraps a {@link InMemoryRepositoryTransactionManager} to coordinate transactions across all
 * registered repositories. Each call to {@link #register(Object, RepositoryLifecycle)} creates a
 * {@link InMemoryTransactionalRepository} internally and returns a {@link RepositoryHandle} that
 * also implements the package-private {@link WritableHandle} interface, allowing
 * {@link org.opentripplanner.framework.snapshot.transaction.internal.DefaultWriteContext} to obtain
 * mutable snapshot access via an internal cast without exposing it on the public
 * {@link RepositoryHandle} API.
 */
class DefaultRepositoryRegistry implements RepositoryRegistry {

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
    return new WritableRepositoryHandle<>(repo);
  }

  @Override
  public RepositoryScope scope() {
    return new DefaultRepositoryScope(transactionManager.requestScopedTransaction());
  }

  /**
   * Returns the transaction manager for use during wiring of the
   * {@link UpdateManager}.
   */
  InMemoryRepositoryTransactionManager transactionManager() {
    return transactionManager;
  }

  /**
   * A {@link RepositoryHandle} that also implements {@link WritableHandle}, allowing the
   * {@link org.opentripplanner.framework.snapshot.update.internal.DefaultWriteContext} to obtain
   * mutable snapshot access via an internal cast.
   */
  private static class WritableRepositoryHandle<S, T>
    implements RepositoryHandle<S, T>, WritableHandle<T> {

    private final TransactionalRepository<S, T> repo;

    WritableRepositoryHandle(TransactionalRepository<S, T> repo) {
      this.repo = repo;
    }

    @Override
    public S readOnlySnapshot(Transaction transaction) {
      return repo.snapshot(transaction);
    }

    @Override
    public Supplier<T> mutableSnapshot() {
      return ((InMemoryTransactionalRepository<S, T>) repo).mutableSnapshot();
    }
  }
}
