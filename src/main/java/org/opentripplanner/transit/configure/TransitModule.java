package org.opentripplanner.transit.configure;

import dagger.Module;
import dagger.Provides;
import org.opentripplanner.routing.graph.GraphModel;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.transit.service.TransitModel;

@Module
public abstract class TransitModule {

  @Provides
  public static TransitModel provideTransitModel(GraphModel graphModel, Deduplicator deduplicator) {
    return new TransitModel(graphModel.graph().getStopModel(), deduplicator);
  }
}
