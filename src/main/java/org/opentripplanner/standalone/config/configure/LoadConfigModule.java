package org.opentripplanner.standalone.config.configure;

import dagger.Module;
import dagger.Provides;
import java.io.File;
import javax.inject.Singleton;
import org.opentripplanner.datastore.api.OtpBaseDirectory;
import org.opentripplanner.datastore.api.OtpDataStoreConfig;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.OtpConfigLoader;
import org.opentripplanner.standalone.config.api.TransitServicePeriod;

/**
 * Load and create the {@link ConfigModel} using the provided configuration file
 * directory.
 * <p>
 * The included {@link ConfigModule} is used to bind/map {@link ConfigModel} to more specific
 * types. The {@link ConfigModule} is a separate module to be able to use it without this module;
 * If the {@link ConfigModel} is already instantiated.
 * <p>
 * The binding to {@link OtpDataStoreConfig} and {@link @TransitServicePeriod} is done
 * here, not in the {@link ConfigModel}, because they are only needed at load time - if this change,
 * then move the binding to the {@link ConfigModule}.
 */
@Module(includes = ConfigModule.class)
public class LoadConfigModule {

  @Provides
  static OtpConfigLoader providesConfigLoader(@OtpBaseDirectory File configDirectory) {
    return new OtpConfigLoader(configDirectory);
  }

  @Provides
  @Singleton
  static ConfigModel providesModel(OtpConfigLoader loader) {
    return new ConfigModel(loader);
  }

  @Provides
  static OtpDataStoreConfig providesDataStoreConfig(BuildConfig buildConfig) {
    return buildConfig;
  }

  @Provides
  @TransitServicePeriod
  static ServiceDateInterval providesTransitServicePeriod(BuildConfig buildConfig) {
    return buildConfig.getTransitServicePeriod();
  }

  @Provides
  @OtpBaseDirectory
  static File baseDirectory(CommandLineParameters cli) {
    return cli.getBaseDirectory();
  }
}
