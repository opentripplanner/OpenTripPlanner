package org.opentripplanner.framework.snapshot.transaction;

public interface TransactionalRepository<S, T> {
  S snapshot(Transaction transaction);
}
