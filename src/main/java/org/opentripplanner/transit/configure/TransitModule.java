package org.opentripplanner.transit.configure;

import dagger.Module;
import dagger.Provides;
import org.opentripplanner.routing.graph.GraphModel;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.TransitModel;

@Module
public abstract class TransitModule {

  @Provides
  public static TransitModel provideTransitModel(GraphModel graphModel, Deduplicator deduplicator) {
    return new TransitModel(graphModel.graph().getStopModel(), deduplicator);
  }
}
