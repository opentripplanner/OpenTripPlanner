package org.opentripplanner.routing.graph.configure;

import dagger.Module;
import dagger.Provides;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Singleton;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.StopModel;

@Module
public abstract class GraphModule {

  /** We wrap the graph to be able to set it after loading the serialized state. */
  @Provides
  @Singleton
  public static AtomicReference<Graph> provideGraph(
    BuildConfig config,
    Deduplicator deduplicator,
    StopModel stopModel
  ) {
    var graph = new Graph(stopModel, deduplicator);
    graph.initOpeningHoursCalendarService(config.getTransitServicePeriod());
    return new AtomicReference<>(graph);
  }
}
