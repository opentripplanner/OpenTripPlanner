package org.opentripplanner.service.elevatorstatus.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.core.framework.transaction.configure.TransitDomain;
import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.RepositoryRegistry;
import org.opentripplanner.service.elevatorstatus.ElevatorStatusRepository;
import org.opentripplanner.service.elevatorstatus.ElevatorStatusRepositorySnapshot;
import org.opentripplanner.service.elevatorstatus.internal.DefaultElevatorStatusRepository;
import org.opentripplanner.service.elevatorstatus.internal.ElevatorStatusRepositoryLifecycle;

/**
 * Registers the elevator status repository on the transit domain's transaction framework. It is
 * registered there, and not on the street domain, so a status change and the transit data that
 * depends on it are committed in one transaction. All elevators are in service at startup.
 */
@Module
public abstract class ElevatorStatusRepositoryModule {

  @Provides
  @Singleton
  public static RepositoryHandle<
    ElevatorStatusRepositorySnapshot,
    ElevatorStatusRepository
  > elevatorStatusRepositoryHandle(@TransitDomain RepositoryRegistry repositoryRegistry) {
    return repositoryRegistry.registerRepository(
      new DefaultElevatorStatusRepository(),
      new ElevatorStatusRepositoryLifecycle()
    );
  }
}
