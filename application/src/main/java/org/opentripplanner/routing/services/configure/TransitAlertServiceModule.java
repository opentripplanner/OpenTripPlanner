package org.opentripplanner.routing.services.configure;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.routing.impl.DelegatingTransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;

/**
 * Wires up the application-wide {@link TransitAlertService}. The service is a delegating aggregator
 * that combines the alerts from all realtime updaters. The concrete type is exposed so that the
 * updater configuration can register the per-updater services into it.
 */
@Module
public abstract class TransitAlertServiceModule {

  @Binds
  abstract TransitAlertService bindTransitAlertService(DelegatingTransitAlertServiceImpl service);

  @Provides
  @Singleton
  static DelegatingTransitAlertServiceImpl transitAlertService() {
    return new DelegatingTransitAlertServiceImpl();
  }
}
