package org.opentripplanner.standalone.configure;

import dagger.BindsInstance;
import dagger.Component;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.ext.emissions.EmissionsServiceModule;
import org.opentripplanner.ext.interactivelauncher.configuration.InteractiveLauncherModule;
import org.opentripplanner.ext.ridehailing.configure.RideHailingServicesModule;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.configure.StopConsolidationServiceModule;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.configure.RealtimeVehicleRepositoryModule;
import org.opentripplanner.service.realtimevehicles.configure.RealtimeVehicleServiceModule;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.vehiclerental.configure.VehicleRentalRepositoryModule;
import org.opentripplanner.service.vehiclerental.configure.VehicleRentalServiceModule;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.configure.WorldEnvelopeServiceModule;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.configure.ConfigModule;
import org.opentripplanner.standalone.server.MetricsLogging;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.street.service.StreetLimitationParametersServiceModule;
import org.opentripplanner.transit.configure.TransitModule;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.visualizer.GraphVisualizer;

/**
 * Dagger dependency injection Factory to create components for the OTP construct application phase.
 */
@Singleton
@Component(
  modules = {
    ConfigModule.class,
    TransitModule.class,
    WorldEnvelopeServiceModule.class,
    RealtimeVehicleServiceModule.class,
    RealtimeVehicleRepositoryModule.class,
    VehicleRentalServiceModule.class,
    VehicleRentalRepositoryModule.class,
    ConstructApplicationModule.class,
    RideHailingServicesModule.class,
    EmissionsServiceModule.class,
    StopConsolidationServiceModule.class,
    InteractiveLauncherModule.class,
    StreetLimitationParametersServiceModule.class,
  }
)
public interface ConstructApplicationFactory {
  ConfigModel config();
  RaptorConfig<TripSchedule> raptorConfig();
  Graph graph();
  TransitModel transitModel();
  WorldEnvelopeRepository worldEnvelopeRepository();
  WorldEnvelopeService worldEnvelopeService();
  RealtimeVehicleRepository realtimeVehicleRepository();
  RealtimeVehicleService realtimeVehicleService();
  VehicleRentalRepository vehicleRentalRepository();
  VehicleRentalService vehicleRentalService();
  DataImportIssueSummary dataImportIssueSummary();

  @Nullable
  EmissionsDataModel emissionsDataModel();

  @Nullable
  GraphVisualizer graphVisualizer();

  TransitService transitService();
  OtpServerRequestContext createServerContext();

  MetricsLogging metricsLogging();

  @Nullable
  StopConsolidationRepository stopConsolidationRepository();

  StreetLimitationParameters streetLimitationParameters();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder configModel(ConfigModel config);

    @BindsInstance
    Builder graph(Graph graph);

    @BindsInstance
    Builder transitModel(TransitModel transitModel);

    @BindsInstance
    Builder graphVisualizer(@Nullable GraphVisualizer graphVisualizer);

    @BindsInstance
    Builder worldEnvelopeRepository(WorldEnvelopeRepository worldEnvelopeRepository);

    @BindsInstance
    Builder stopConsolidationRepository(
      @Nullable StopConsolidationRepository stopConsolidationRepository
    );

    @BindsInstance
    Builder dataImportIssueSummary(DataImportIssueSummary issueSummary);

    @BindsInstance
    Builder emissionsDataModel(EmissionsDataModel emissionsDataModel);

    @BindsInstance
    Builder streetLimitationParameters(StreetLimitationParameters streetLimitationParameters);

    ConstructApplicationFactory build();
  }
}
