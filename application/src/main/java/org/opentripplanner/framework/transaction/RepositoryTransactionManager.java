package org.opentripplanner.framework.transaction;

public interface RepositoryTransactionManager {

  public Transaction requestScopedTransaction();

  public void commit();
}
