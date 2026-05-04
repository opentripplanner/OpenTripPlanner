package org.opentripplanner.framework.snapshot.transaction;

import org.opentripplanner.framework.snapshot.transaction.internal.DefaultRepositoryRegistry;

public class TransactionConfig {

  public static RepositoryRegistry createRepositoryRegistry() {
    return new DefaultRepositoryRegistry();
  }
}
