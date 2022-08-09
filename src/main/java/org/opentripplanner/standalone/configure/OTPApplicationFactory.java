package org.opentripplanner.standalone.configure;

import dagger.BindsInstance;
import dagger.Component;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Singleton;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.datastore.configure.DataStoreModule;
import org.opentripplanner.ext.datastore.gs.GsDataSourceModule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.configure.GraphModule;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.ConfigModule;
import org.opentripplanner.standalone.config.OtpBaseDirectory;
import org.opentripplanner.transit.configure.TransitModule;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;
import org.opentripplanner.transit.service.TransitModel;

/**
 * This abstract class provide the top level service created and wired together using the Dagger 2
 * Dependency Injection framework. Dagger picks up this class and implement it.
 */
@Singleton
@Component(
  modules = {
    AppModule.class,
    ConfigModule.class,
    DataStoreModule.class,
    GraphModule.class,
    GsDataSourceModule.class,
    TransitModule.class,
  }
)
public interface OTPApplicationFactory {
  RaptorConfig<TripSchedule> raptorConfig();
  OtpDataStore datastore();
  ConfigModel configModel();
  Deduplicator deduplicator();
  AtomicReference<Graph> graph();
  AtomicReference<TransitModel> transitModel();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder baseDirectory(@OtpBaseDirectory File baseDirectory);

    OTPApplicationFactory build();
  }
}
