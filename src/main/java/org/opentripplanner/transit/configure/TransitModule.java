package org.opentripplanner.transit.configure;

import dagger.Module;
import dagger.Provides;
import java.util.concurrent.atomic.AtomicReference;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

@Module
public abstract class TransitModule {

  /** We wrap the graph to be able to set it after loading the serialized state. */
  @Provides
  public static AtomicReference<TransitModel> provideTransitModel(
    StopModel stopModel,
    Deduplicator deduplicator
  ) {
    return new AtomicReference<>(new TransitModel(stopModel, deduplicator));
  }
}
