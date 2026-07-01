package org.opentripplanner.framework.transaction.internal;

import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.transaction.UpdateManager;
import org.opentripplanner.framework.transaction.api.TransactionScope;

/**
 * An opaque identity token representing a single committed state of the repository set.
 *
 * <p>A new token is minted on every {@link UpdateManager#submit(Consumer)} call. Readers that
 * captured an older token continue to see the state as of that commit; readers that create a new
 * {@link TransactionScope} after the commit see the updated state.
 */
final class Transaction {

  private final long id;

  Transaction(long id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Transaction that = (Transaction) o;
    return id == that.id;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return "TXN-" + id;
  }
}
