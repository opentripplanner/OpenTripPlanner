package org.opentripplanner.standalone.configure;

import dagger.BindsInstance;
import dagger.Component;
import jakarta.inject.Singleton;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.datastore.configure.DataStoreModule;
import org.opentripplanner.ext.datastore.gs.GsDataSourceModule;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.configure.StopConsolidationRepositoryModule;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.configure.OsmInfoGraphBuildRepositoryModule;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.configure.VehicleParkingRepositoryModule;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.configure.WorldEnvelopeRepositoryModule;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.configure.LoadConfigModule;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.transit.service.TimetableRepository;

/**
 * Dagger dependency injection Factory to create components for the OTP load application phase.
 */
@Singleton
@Component(
  modules = {
    LoadConfigModule.class,
    DataStoreModule.class,
    GsDataSourceModule.class,
    OsmInfoGraphBuildRepositoryModule.class,
    WorldEnvelopeRepositoryModule.class,
    StopConsolidationRepositoryModule.class,
    VehicleParkingRepositoryModule.class,
  }
)
public interface LoadApplicationFactory {
  OtpDataStore datastore();

  ConfigModel configModel();

  @Singleton
  Graph emptyGraph();

  @Singleton
  OsmInfoGraphBuildRepository emptyOsmInfoGraphBuildRepository();

  @Singleton
  TimetableRepository emptyTimetableRepository();

  @Singleton
  WorldEnvelopeRepository emptyWorldEnvelopeRepository();

  @Singleton
  GraphBuilderDataSources graphBuilderDataSources();

  @Singleton
  EmissionsDataModel emptyEmissionsDataModel();

  @Singleton
  StopConsolidationRepository emptyStopConsolidationRepository();

  @Singleton
  StreetLimitationParameters emptyStreetLimitationParameters();

  @Singleton
  VehicleParkingRepository emptyVehicleParkingRepository();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder commandLineParameters(CommandLineParameters cli);

    LoadApplicationFactory build();
  }
}
