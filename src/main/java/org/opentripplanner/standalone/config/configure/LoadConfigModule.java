package org.opentripplanner.standalone.config.configure;

import dagger.Module;
import dagger.Provides;
import java.io.File;
import javax.inject.Singleton;
import org.opentripplanner.datastore.api.OtpDataStoreConfig;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.ConfigLoader;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.api.OtpBaseDirectory;
import org.opentripplanner.standalone.config.api.TransitServicePeriod;

@Module
public class LoadConfigModule {

  @Provides
  @Singleton
  static ConfigModel provideModel(ConfigLoader loader) {
    return new ConfigModel(loader);
  }

  @Provides
  static OtpDataStoreConfig provideDataStoreConfig(BuildConfig buildConfig) {
    return buildConfig.storage;
  }

  @Provides
  static ConfigLoader provideConfigLoader(@OtpBaseDirectory File configDirectory) {
    return new ConfigLoader(configDirectory);
  }

  @Provides
  @TransitServicePeriod
  static ServiceDateInterval transitServicePeriod(BuildConfig buildConfig) {
    return buildConfig.getTransitServicePeriod();
  }
}
