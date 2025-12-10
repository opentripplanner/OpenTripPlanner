package org.opentripplanner.framework.transaction;

public interface RepositoryLifecycle<T, U> {
  U copyOnWrite(T readOnlyRepository);
  T freeze(U editableReopsitory);
}
