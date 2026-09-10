package org.opentripplanner.service.transitalert.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.configure.AlertDomain;
import org.opentripplanner.service.transitalert.TransitAlertRepository;
import org.opentripplanner.service.transitalert.TransitAlertRepositorySnapshot;
import org.opentripplanner.service.transitalert.internal.DefaultTransitAlertRepository;
import org.opentripplanner.service.transitalert.internal.TransitAlertRepositoryLifecycle;

@Module
public abstract class TransitAlertRepositoryModule {

  @Provides
  @Singleton
  public static RepositoryHandle<
    TransitAlertRepositorySnapshot,
    TransitAlertRepository
  > transitAlertRepositoryHandle(@AlertDomain RepositoryRegistry repositoryRegistry) {
    return repositoryRegistry.registerRepository(
      new DefaultTransitAlertRepository(),
      new TransitAlertRepositoryLifecycle()
    );
  }
}
