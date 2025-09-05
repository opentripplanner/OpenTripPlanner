package org.opentripplanner.ext.empiricaldelay.configure;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nullable;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayRepository;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayService;
import org.opentripplanner.ext.empiricaldelay.internal.DefaultEmpiricalDelayService;
import org.opentripplanner.framework.application.OTPFeature;

/**
 * The service is used during application serve phase, not loading, so we need to provide
 * a module for the service without the repository, which is injected from the loading phase.
 */
@Module
public class EmpiricalDelayServiceModule {

  @Provides
  @Nullable
  public EmpiricalDelayService provideEmpericalDelayService(EmpiricalDelayRepository repository) {
    // The repository could be null if the feature is turned of after graph serialization
    if (OTPFeature.EmpiricalDelay.isOff() || repository == null) {
      return null;
    }
    return new DefaultEmpiricalDelayService(repository);
  }
}
