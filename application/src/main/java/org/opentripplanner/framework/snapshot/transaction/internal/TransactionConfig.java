package org.opentripplanner.framework.snapshot.transaction.internal;

import org.opentripplanner.framework.snapshot.transaction.RepositoryRegistry;
import org.opentripplanner.framework.snapshot.transaction.UpdateManager;

public class TransactionConfig {

  public static RepositoryRegistry createRepositoryRegistry() {
    return new DefaultRepositoryRegistry();
  }

  public static UpdateManager createUpdateManager(RepositoryRegistry registry) {
    return new DefaultUpdateManager(((DefaultRepositoryRegistry) registry).transactionManager());
  }
}
