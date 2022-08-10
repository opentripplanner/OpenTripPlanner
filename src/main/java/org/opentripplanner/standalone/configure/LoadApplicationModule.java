package org.opentripplanner.standalone.configure;

import dagger.Module;
import dagger.Provides;
import java.io.File;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.api.OtpBaseDirectory;
import org.opentripplanner.standalone.config.api.TransitServicePeriod;

/**
 * Main focus is to provide bindings not provided elsewhere for the load application phase. For
 * example binding specific config parameters to DI managed input parameters. This allows us to keep
 * avoid passing entire configuration into modules, creating undecided dependencies.
 */
@Module
public class LoadApplicationModule {

  @Provides
  @OtpBaseDirectory
  static File baseDirectory(CommandLineParameters cli) {
    return cli.getBaseDirectory();
  }

  @Provides
  @TransitServicePeriod
  static ServiceDateInterval transitServicePeriod(BuildConfig buildConfig) {
    return buildConfig.getTransitServicePeriod();
  }
}
