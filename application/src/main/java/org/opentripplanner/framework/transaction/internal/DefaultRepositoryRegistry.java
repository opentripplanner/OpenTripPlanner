package org.opentripplanner.framework.transaction.internal;

import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.UpdateManager;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;
import org.opentripplanner.framework.transaction.api.TransactionScope;

/**
 * Default implementation of {@link RepositoryRegistry}.
 *
 * <p>Wraps a {@link TransactionManager} to coordinate transactions across all registered
 * repositories. Each call to {@link #registerRepositorySnapshot(Object, RepositoryLifecycle)} creates a
 * {@link TransactionalRepository} and wraps it in a {@link DefaultRepositoryHandle}, which exposes
 * mutable access to {@link DefaultWriteContext} via an internal cast without surfacing it on the
 * public {@link RepositoryHandle} API.
 */
class DefaultRepositoryRegistry implements RepositoryRegistry {

  private final TransactionManager transactionManager = new TransactionManager();

  @Override
  public <S, M> RepositoryHandle<S, M> registerRepositorySnapshot(
    S initialRepositorySnapshot,
    RepositoryLifecycle<S, M> lifecycle
  ) {
    TransactionalRepository<S, M> repo = new TransactionalRepository<>(
      initialRepositorySnapshot,
      lifecycle,
      transactionManager
    );
    return new DefaultRepositoryHandle<>(repo);
  }

  @Override
  public <S, M> RepositoryHandle<S, M> registerRepository(
    M repository,
    RepositoryLifecycle<S, M> lifecycle
  ) {
    return registerRepositorySnapshot(lifecycle.freeze(repository), lifecycle);
  }

  @Override
  public TransactionScope scope() {
    return new DefaultTransactionScope(transactionManager.requestScopedTransaction());
  }

  /**
   * Returns the transaction manager for use during wiring of the
   * {@link UpdateManager}.
   */
  TransactionManager transactionManager() {
    return transactionManager;
  }
}
