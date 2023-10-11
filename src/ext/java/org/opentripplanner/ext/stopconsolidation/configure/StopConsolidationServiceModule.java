package org.opentripplanner.ext.stopconsolidation.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationService;

@Module
public interface StopConsolidationServiceModule {
  @Binds
  StopConsolidationService bindRepository(DefaultStopConsolidationService repo);
}
