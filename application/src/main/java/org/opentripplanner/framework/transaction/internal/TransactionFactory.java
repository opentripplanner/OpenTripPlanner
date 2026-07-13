package org.opentripplanner.framework.transaction.internal;

import java.time.Duration;
import java.util.concurrent.ThreadFactory;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.UpdateManager;

/**
 * Factory for creating instances of the transaction framework.
 *
 * <p>Use this as the entry point for wiring the framework in a Dagger module or similar setup.
 * Create a {@link RepositoryRegistry} first, register repositories on it, then create an
 * {@link UpdateManager} bound to the same registry.
 */
public class TransactionFactory {

  public static RepositoryRegistry createRepositoryRegistry() {
    return new DefaultRepositoryRegistry();
  }

  public static UpdateManager createUpdateManagerWithAtomicCommits(
    String name,
    RepositoryRegistry registry,
    ThreadFactory threadFactory
  ) {
    return new DefaultUpdateManager(
      name,
      ((DefaultRepositoryRegistry) registry).transactionManager(),
      threadFactory,
      null
    );
  }

  public static UpdateManager createUpdateManagerWithPeriodicCommits(
    String name,
    RepositoryRegistry registry,
    ThreadFactory threadFactory,
    Duration commitInterval
  ) {
    return new DefaultUpdateManager(
      name,
      ((DefaultRepositoryRegistry) registry).transactionManager(),
      threadFactory,
      commitInterval
    );
  }
}
