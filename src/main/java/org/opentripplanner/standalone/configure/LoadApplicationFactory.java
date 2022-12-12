package org.opentripplanner.standalone.configure;

import dagger.BindsInstance;
import dagger.Component;
import javax.inject.Singleton;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.datastore.configure.DataStoreModule;
import org.opentripplanner.ext.datastore.gs.GsDataSourceModule;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.worldenvelope.service.WorldEnvelopeModel;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.configure.LoadConfigModule;
import org.opentripplanner.transit.service.TransitModel;

/**
 * Dagger dependency injection Factory to create components for the OTP load application phase.
 */
@Singleton
@Component(modules = { LoadConfigModule.class, DataStoreModule.class, GsDataSourceModule.class })
public interface LoadApplicationFactory {
  OtpDataStore datastore();

  ConfigModel configModel();

  @Singleton
  Graph emptyGraph();

  @Singleton
  TransitModel emptyTransitModel();

  @Singleton
  WorldEnvelopeModel emptyWorldWorldEnvelopeModel();

  @Singleton
  GraphBuilderDataSources graphBuilderDataSources();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder commandLineParameters(CommandLineParameters cli);

    LoadApplicationFactory build();
  }
}
