package org.opentripplanner.routing.graph.configure;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphModel;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.StopModel;

@Module
public abstract class GraphModule {

  @Provides
  @Singleton
  public static GraphModel provideGraph(BuildConfig config, Deduplicator deduplicator) {
    var graph = new Graph(new StopModel(), deduplicator);
    graph.initOpeningHoursCalendarService(config.getTransitServicePeriod());
    return new GraphModel(graph);
  }

  @Provides
  @Singleton
  public static Deduplicator provideDeduplicator() {
    return new Deduplicator();
  }
}
