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
  public static AtomicReference<Graph> provideGraph(BuildConfig config, Deduplicator deduplicator) {
    var graph = new Graph(new StopModel(), deduplicator);
    graph.initOpeningHoursCalendarService(config.getTransitServicePeriod());
    return new AtomicReference<>(graph);
  }

  @Provides
  @Singleton
  public static Deduplicator provideDeduplicator() {
    return new Deduplicator();
  }

  @Provides
  @Singleton
  public static StopModel stopModel() {
    return new StopModel();
  }
}
