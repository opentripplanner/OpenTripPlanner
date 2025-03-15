package org.opentripplanner.ext.emissions.configure;

import dagger.Module;
import dagger.Provides;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.ext.emissions.internal.DefaultEmissionsService;
import org.opentripplanner.ext.emissions.itinerary.DecorateWithEmission;
import org.opentripplanner.routing.algorithm.filterchain.ext.EmissionsDecorator;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;

/**
 * The service is used during application serve phase, not loading, so we need to provide
 * a module for the service without the repository, which is injected from the loading phase.
 */
@Module
public class EmissionsServiceModule {

  @Provides
  @EmissionsDecorator
  public ItineraryDecorator provideEmissionsService(EmissionsDataModel emissionsDataModel) {
    return new DecorateWithEmission(new DefaultEmissionsService(emissionsDataModel));
  }
}
