package org.opentripplanner.standalone.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.fares.FareServiceFactory;

@Module(subcomponents = RequestScopedComponent.class)
public class ConstructApplicationModule {

  @Singleton
  @Provides
  public FareService fareService(FareServiceFactory fareServiceFactory) {
    return fareServiceFactory.makeFareService();
  }
}
