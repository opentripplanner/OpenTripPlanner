package org.opentripplanner.standalone.configure;

import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;

import dagger.BindsInstance;
import dagger.Component;
import java.io.File;
import javax.inject.Singleton;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.datastore.configure.DataStoreModule;
import org.opentripplanner.ext.datastore.gs.GsDataSourceModule;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.ConfigModule;
import org.opentripplanner.standalone.config.OtpBaseDirectory;
import org.opentripplanner.standalone.config.OtpConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.OTPServer;

/**
 * This class is responsible for creating the top level services like the {@link OTPServer}. The
 * purpose of this class is to wire the application, creating the necessary Services and modules
 * and putting them together using the Dagger 2 Dependency Injection framework . It is NOT
 * responsible for starting or running the application.
 * <p>
 * THIS CLASS IS NOT THREAD SAFE - THE APPLICATION SHOULD BE CREATED IN ONE THREAD. This should be
 * really fast, since the only IO operations are reading config files and logging. Loading transit
 * or map data should NOT happen during this phase.
 */
@Singleton
@Component(modules = { ConfigModule.class, DataStoreModule.class, GsDataSourceModule.class })
public interface OTPApplicationFactory {
  OtpConfig otpConfig();
  BuildConfig buildConfig();
  RouterConfig routerConfig();
  OtpDataStore datastore();
  ConfigModel configModel();

  default void setOtpConfigVersionsOnServerInfo() {
    projectInfo().otpConfigVersion = otpConfig().configVersion;
    projectInfo().buildConfigVersion = buildConfig().configVersion;
    projectInfo().routerConfigVersion = routerConfig().getConfigVersion();
  }

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder baseDirectory(@OtpBaseDirectory File baseDirectory);

    OTPApplicationFactory build();
  }
}
