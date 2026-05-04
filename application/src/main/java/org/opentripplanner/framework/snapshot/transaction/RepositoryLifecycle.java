package org.opentripplanner.framework.snapshot.transaction;

public interface RepositoryLifecycle<S, M> {

  M copyOnWrite(S readOnlySnapshot);

  S freeze(M mutableSnapshot);
}
