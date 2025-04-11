package org.opentripplanner.ext.emission.configure;

import dagger.Module;
import dagger.Provides;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.internal.DefaultEmissionService;
import org.opentripplanner.ext.emission.internal.itinerary.EmissionItineraryDecorator;
import org.opentripplanner.routing.algorithm.filterchain.ext.EmissionDecorator;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;

/**
 * The service is used during application serve phase, not loading, so we need to provide
 * a module for the service without the repository, which is injected from the loading phase.
 */
@Module
public class EmissionServiceModule {

  @Provides
  @EmissionDecorator
  public ItineraryDecorator provideEmissionService(EmissionRepository emissionRepository) {
    return new EmissionItineraryDecorator(new DefaultEmissionService(emissionRepository));
  }
}
