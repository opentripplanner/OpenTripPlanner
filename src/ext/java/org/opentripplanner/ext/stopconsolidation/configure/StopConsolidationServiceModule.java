package org.opentripplanner.ext.stopconsolidation.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationService;
import org.opentripplanner.transit.service.TransitModel;

@Module
public class StopConsolidationServiceModule {

  @Provides
  @Singleton
  @Nullable
  StopConsolidationService service(@Nullable StopConsolidationRepository repo, TransitModel tm) {
    if (repo == null) {
      return null;
    } else {
      return new DefaultStopConsolidationService(repo, tm);
    }
  }
}
