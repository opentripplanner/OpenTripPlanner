package org.opentripplanner.standalone.configure;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import org.opentripplanner.datastore.api.OtpDataStoreConfig;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.OtpConfig;
import org.opentripplanner.standalone.config.RouterConfig;

@Module
public abstract class OTPApplicationModule {

  @Provides
  @Singleton
  static OTPConfiguration provideOtpConfiguration(CommandLineParameters commandLineParameters) {
    return new OTPConfiguration(commandLineParameters);
  }

  @Provides
  @Singleton
  static OtpConfig provideOtpConfig(OTPConfiguration configuration) {
    return configuration.otpConfig();
  }

  @Provides
  @Singleton
  static BuildConfig provideBuildConfig(OTPConfiguration configuration) {
    return configuration.buildConfig();
  }

  @Provides
  @Singleton
  static RouterConfig provideRouterConfig(OTPConfiguration configuration) {
    return configuration.routerConfig();
  }

  @Provides
  @Singleton
  static OtpDataStoreConfig provideDataStoreConfig(OTPConfiguration configuration) {
    return configuration.createDataStoreConfig();
  }
}
