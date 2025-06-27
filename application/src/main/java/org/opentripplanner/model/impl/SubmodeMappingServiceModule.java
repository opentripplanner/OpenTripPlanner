package org.opentripplanner.model.impl;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.transit.service.TimetableRepository;

@Module
public class SubmodeMappingServiceModule {

  @Provides
  @Singleton
  public static SubmodeMappingService submodeMappingService(
    TimetableRepository timetableRepository
  ) {
    return new SubmodeMappingService(timetableRepository.getSubmodeMapping());
  }
}
