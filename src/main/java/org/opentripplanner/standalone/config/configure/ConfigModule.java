package org.opentripplanner.standalone.config.configure;

import dagger.Module;
import dagger.Provides;
import java.io.File;
import javax.inject.Singleton;
import org.opentripplanner.datastore.api.OtpDataStoreConfig;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.ConfigLoader;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.OtpConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.api.OtpBaseDirectory;

@Module
public class ConfigModule {

  @Provides
  @Singleton
  static ConfigModel provideModel(ConfigLoader loader) {
    return new ConfigModel(loader);
  }

  @Provides
  static OtpConfig provideOtpConfig(ConfigModel model) {
    return model.otpConfig();
  }

  @Provides
  static BuildConfig provideBuildConfig(ConfigModel model) {
    return model.buildConfig();
  }

  @Provides
  static RouterConfig provideRouterConfig(ConfigModel model) {
    return model.routerConfig();
  }

  @Provides
  static OtpDataStoreConfig provideDataStoreConfig(BuildConfig buildConfig) {
    return buildConfig.storage;
  }

  @Provides
  static ConfigLoader provideConfigLoader(@OtpBaseDirectory File configDirectory) {
    return new ConfigLoader(configDirectory);
  }
}
