package org.opentripplanner.framework.transaction;

import java.util.function.Supplier;

public interface TransactionalRepository<S, T> {

  public S snapshot(Transaction transaction);

  public Supplier<T> mutableSnapshot();
}
