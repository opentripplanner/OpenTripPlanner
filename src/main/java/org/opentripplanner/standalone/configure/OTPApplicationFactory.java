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

/**
 * This abstract class provide the top level service created and wired together using the Dagger 2
 * Dependency Injection framework. Dagger picks up this class and implement it.
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
