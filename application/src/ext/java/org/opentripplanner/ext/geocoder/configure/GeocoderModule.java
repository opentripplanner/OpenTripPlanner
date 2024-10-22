package org.opentripplanner.ext.geocoder.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.transit.service.TimetableRepository;

/**
 * This module builds the Lucene geocoder based on whether the feature flag is on or off.
 */
@Module
public class GeocoderModule {

  @Provides
  @Singleton
  @Nullable
  LuceneIndex luceneIndex(
    TimetableRepository timetableRepository,
    @Nullable StopConsolidationService stopConsolidationService
  ) {
    if (OTPFeature.SandboxAPIGeocoder.isOn()) {
      return new LuceneIndex(timetableRepository, stopConsolidationService);
    } else {
      return null;
    }
  }
}
