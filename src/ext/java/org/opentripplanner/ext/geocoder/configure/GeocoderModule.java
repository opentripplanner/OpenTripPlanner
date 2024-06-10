package org.opentripplanner.ext.geocoder.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.transit.service.TransitService;

/**
 * This module converts the ride hailing configurations into ride hailing services to be used by the
 * application context.
 */
@Module
public class GeocoderModule {

  @Provides
  @Singleton
  @Nullable
  LuceneIndex luceneIndex(TransitService service) {
    if (OTPFeature.SandboxAPIGeocoder.isOn()) {
      return new LuceneIndex(service);
    } else {
      return null;
    }
  }
}
