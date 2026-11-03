package org.opentripplanner.ext.stopconsolidation.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationService;
import org.opentripplanner.transit.service.TransitRepository;

@Module
public class StopConsolidationServiceModule {

  @Provides
  @Singleton
  @Nullable
  StopConsolidationService service(
    @Nullable StopConsolidationRepository repo,
    TransitRepository tm
  ) {
    if (repo == null) {
      return null;
    } else {
      return new DefaultStopConsolidationService(repo, tm);
    }
  }
}
