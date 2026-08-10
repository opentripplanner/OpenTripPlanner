package org.opentripplanner.framework.transaction.api;

/**
 * Strategy for the copy-on-write / freeze lifecycle of a repository snapshot.
 *
 * <p>A lifecycle is registered once per repository. The framework calls
 * {@link #copyOnWrite(Object)} when a write task first accesses a repository within a transaction,
 * and {@link #freeze(Object)} at commit time to produce the new repository snapshot.
 *
 * @param <S> the read-only repository snapshot type
 * @param <M> the mutable repository type
 */
public interface RepositoryLifecycle<S, M> {
  /**
   * Create a mutable copy of the given repository (snapshot). The returned instance is reused for
   * all writes within the same transaction.
   */
  M copyOnWrite(S repositorySnapshot);

  /**
   * Produce a new repository snapshot from the given repository.
   *
   * <p>Called when a transaction commits. After this call the repository is discarded;
   * the returned repository snapshot becomes the visible state for the next transaction.
   */
  S freeze(M repository);
}
