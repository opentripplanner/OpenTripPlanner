package org.opentripplanner.framework.transaction.internal;

import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.TransactionScope;

/**
 * Package-private implementation of {@link RepositoryHandle} that additionally exposes
 * {@link #repository()} for write access within the package. {@link DefaultWriteContext}
 * casts to this type to obtain the mutable repository without exposing write access on the
 * public {@link RepositoryHandle} API.
 */
class DefaultRepositoryHandle<S, M> implements RepositoryHandle<S, M> {

  private final TransactionalRepository<S, M> repo;

  DefaultRepositoryHandle(TransactionalRepository<S, M> repo) {
    this.repo = repo;
  }

  @Override
  public S repositorySnapshot(TransactionScope scope) {
    var transaction = ((DefaultTransactionScope) scope).transaction();
    var snapshot = repo.repositorySnapshot(transaction);
    if (snapshot == null) {
      throw new IllegalArgumentException(
        "No repository snapshot exists for %s. The scope was most likely created by another " +
          "RepositoryRegistry than the one this repository is registered on - a scope can only be " +
          "used to read repositories of its own write domain.".formatted(scope)
      );
    }
    return snapshot;
  }

  M repository() {
    return repo.repository().get();
  }
}
