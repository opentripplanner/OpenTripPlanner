package org.opentripplanner.ext.stopconsolidation.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationService;
import org.opentripplanner.transit.service.TimetableRepository;

@Module
public class StopConsolidationServiceModule {

  @Provides
  @Singleton
  Optional<StopConsolidationService> service(
    @Nullable StopConsolidationRepository repo,
    TimetableRepository tm
  ) {
    return repo == null
      ? Optional.empty()
      : Optional.of(new DefaultStopConsolidationService(repo, tm));
  }
}
