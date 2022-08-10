package org.opentripplanner.transit.configure;

import dagger.Module;
import dagger.Provides;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Singleton;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

@Module
public abstract class TransitModule {

  /** We wrap the graph to be able to set it after loading the serialized state. */
  @Provides
  @Singleton
  public static AtomicReference<TransitModel> provideTransitModel(Deduplicator deduplicator) {
    return new AtomicReference<>(new TransitModel(new StopModel(), deduplicator));
  }

  @Provides
  @Singleton
  public static Deduplicator provideDeduplicator() {
    return new Deduplicator();
  }

  @Provides
  @Singleton
  public static StopModel stopModel(AtomicReference<TransitModel> transitModel) {
    return transitModel.get().getStopModel();
  }
}
