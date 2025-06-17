package org.opentripplanner.model.impl;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.standalone.config.ConfigModel;

@Module
public class SubmodeMappingModule {

  @Provides
  @Singleton
  public static SubmodeMappingService submodeMappingService(ConfigModel configModel) {
    return new SubmodeMappingService(configModel.routerConfig().getSubmodeMapping());
  }
}
