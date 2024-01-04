package org.opentripplanner.ext.interactivelauncher.configuration;

import dagger.Module;
import dagger.Provides;
import org.opentripplanner.ext.interactivelauncher.api.LauncherRequestDecorator;

@Module
public class InteractiveLauncherModule {

  static LauncherRequestDecorator decorator = request -> request;

  static void enable(LauncherRequestDecorator decorator) {
    InteractiveLauncherModule.decorator = decorator;
  }

  @Provides
  LauncherRequestDecorator requestDecorator() {
    return decorator;
  }
}
