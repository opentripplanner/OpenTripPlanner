package org.opentripplanner.standalone.configure;

import dagger.BindsInstance;
import dagger.Component;
import javax.inject.Singleton;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.datastore.configure.DataStoreModule;
import org.opentripplanner.ext.datastore.gs.GsDataSourceModule;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.server.OTPServer;

/**
 * This class is responsible for creating the top level services like {@link OTPConfiguration} and
 * {@link OTPServer}. The purpose of this class is to wire the application, creating the necessary
 * Services and modules and putting them together. It is NOT responsible for starting or running the
 * application. The whole idea of this class is to separate application construction from running
 * it. Earlier we did this manually, but now this uses the Dependency Injection framework Dagger 2.
 * <p>
 * To create a new
 *
 * <p>
 *
 *
 * <p> The top level construction class(this class) may delegate to other construction classes
 * to inject configuration and services into sub-modules.
 *
 * <p> THIS CLASS IS NOT THREAD SAFE - THE APPLICATION SHOULD BE CREATED IN ONE THREAD. This
 * should be really fast, since the only IO operations are reading config files and logging. Loading
 * transit or map data should NOT happen during this phase.
 */

@Singleton
@Component(
  modules = { OTPApplicationModule.class, DataStoreModule.class, GsDataSourceModule.class }
)
public interface OTPApplicationFactory {
  OTPConfiguration config();

  /** Provide a data store to get access to files, remote or local. */
  OtpDataStore datastore();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder cli(CommandLineParameters cli);

    OTPApplicationFactory build();
  }
}
