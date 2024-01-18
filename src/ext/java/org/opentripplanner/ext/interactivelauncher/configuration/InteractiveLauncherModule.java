package org.opentripplanner.ext.interactivelauncher.configuration;

import dagger.Module;
import dagger.Provides;
import org.opentripplanner.ext.interactivelauncher.api.LauncherRequestDecorator;

@Module
public class InteractiveLauncherModule {

  static LauncherRequestDecorator decorator = request -> request;

  public static void setRequestInterceptor(LauncherRequestDecorator decorator) {
    InteractiveLauncherModule.decorator = decorator;
  }

  @Provides
  LauncherRequestDecorator requestDecorator() {
    return decorator;
  }
}
