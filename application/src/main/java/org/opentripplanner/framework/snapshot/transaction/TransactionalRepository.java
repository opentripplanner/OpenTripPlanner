package org.opentripplanner.framework.snapshot.transaction;

import java.util.function.Supplier;

public interface TransactionalRepository<S, T> {

  S snapshot(Transaction transaction);

  Supplier<T> mutableSnapshot();
}
