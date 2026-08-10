package org.opentripplanner.framework.transaction.configure;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.TimetableSnapshotParameters;
import org.opentripplanner.framework.transaction.UpdateManager;
import org.opentripplanner.framework.transaction.internal.TransactionFactory;

/**
 * Wires one {@code (RepositoryRegistry, UpdateManager)} pair per write domain. Each pair has its
 * own transaction sequence and its own single writer thread, so updaters working on unrelated
 * domains run in parallel. Within one registry there must be exactly one {@link UpdateManager} —
 * a second one would break the exclusive access to the mutable repository buffers between
 * commits.
 */
@Module
public abstract class TransactionModule {

  @Provides
  @Singleton
  @TransitDomain
  public static RepositoryRegistry transitRepositoryRegistry() {
    return TransactionFactory.createRepositoryRegistry();
  }

  @Provides
  @Singleton
  @TransitDomain
  public static UpdateManager transitUpdateManager(
    @TransitDomain RepositoryRegistry repositoryRegistry,
    TimetableSnapshotParameters timetableSnapshotParameters
  ) {
    var threadFactory = new ThreadFactoryBuilder().setNameFormat("transitWriter").build();
    return TransactionFactory.createUpdateManagerWithPeriodicCommits(
      "transit",
      repositoryRegistry,
      threadFactory,
      timetableSnapshotParameters.maxSnapshotFrequency()
    );
  }

  @Provides
  @Singleton
  @StreetDomain
  public static RepositoryRegistry streetRepositoryRegistry() {
    return TransactionFactory.createRepositoryRegistry();
  }

  /**
   * The street domain has no transactional repositories yet, so commits are cheap no-ops and the
   * manager commits atomically after each task. Its value today is the dedicated writer thread:
   * the expensive vehicle-rental linking and geofencing work no longer delays timetable updates.
   * <p>
   * Note that a no-op commit performs no volatile write. Until street repositories join the
   * transaction framework, cross-thread visibility of street-model mutations (edge lists,
   * geofencing boundaries, rental and parking state) relies on the publication built into the
   * street model itself, not on any commit fence — the same guarantees the street model gave
   * before the write-domain split.
   */
  @Provides
  @Singleton
  @StreetDomain
  public static UpdateManager streetUpdateManager(
    @StreetDomain RepositoryRegistry repositoryRegistry
  ) {
    var threadFactory = new ThreadFactoryBuilder().setNameFormat("streetWriter").build();
    return TransactionFactory.createUpdateManagerWithAtomicCommits(
      "street",
      repositoryRegistry,
      threadFactory
    );
  }
}
