package org.opentripplanner.framework.snapshot.transaction;

/**
 * An opaque identity token representing a single committed state of the repository set.
 *
 * <p>A {@code Transaction} is not an ACID transaction — it carries no rollback, isolation, or
 * durability semantics. It exists solely as a {@link java.util.WeakHashMap} key inside each
 * {@link TransactionalRepository}: by holding a strong reference to a {@code Transaction} (via a
 * {@link RepositoryScope}), a caller pins the corresponding read-only snapshots in memory for the
 * duration of a request, and the snapshots are eligible for garbage collection once the scope is
 * released.
 *
 * <p>A new token is minted on every {@link RepositoryRegistry#commit()} call. Readers that
 * captured an older token continue to see the state as of that commit; readers that create a new
 * {@link RepositoryScope} after the commit see the updated state.
 */
public interface Transaction {
}
