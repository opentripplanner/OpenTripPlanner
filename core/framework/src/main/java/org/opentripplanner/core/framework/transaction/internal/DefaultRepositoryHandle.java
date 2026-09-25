package org.opentripplanner.core.framework.transaction.internal;

import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.TransactionScope;

/**
 * Package-private implementation of {@link RepositoryHandle} that additionally exposes
 * {@link #repository()} for write access within the package. {@link DefaultWriteContext} casts to
 * this type to obtain the mutable repository without exposing write access on the public
 * {@link RepositoryHandle} API.
 */
class DefaultRepositoryHandle<S, M> implements RepositoryHandle<S, M> {

  private final TransactionalRepository<S, M> repo;

  DefaultRepositoryHandle(TransactionalRepository<S, M> repo) {
    this.repo = repo;
  }

  @Override
  public S repositorySnapshot(TransactionScope scope) {
    var transaction = ((DefaultTransactionScope) scope).transaction();
    return repo.repositorySnapshot(transaction);
  }

  M repository() {
    return repo.repository().get();
  }
}
