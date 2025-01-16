package org.opentripplanner.standalone.configure;

import dagger.BindsInstance;
import dagger.Component;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.ext.emissions.EmissionsServiceModule;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.geocoder.configure.GeocoderModule;
import org.opentripplanner.ext.interactivelauncher.configuration.InteractiveLauncherModule;
import org.opentripplanner.ext.ridehailing.configure.RideHailingServicesModule;
import org.opentripplanner.ext.sorlandsbanen.SorlandsbanenNorwayService;
import org.opentripplanner.ext.sorlandsbanen.configure.SorlandsbanenNorwayModule;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.configure.StopConsolidationServiceModule;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.configure.RealtimeVehicleRepositoryModule;
import org.opentripplanner.service.realtimevehicles.configure.RealtimeVehicleServiceModule;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehicleparking.configure.VehicleParkingServiceModule;
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
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.visualizer.GraphVisualizer;

/**
 * A Factory used by the Dagger dependency injection system to create the components of OTP, which
 * are then wired up to construct the application.
 */
@Singleton
@Component(
  modules = {
    ConfigModule.class,
    ConstructApplicationModule.class,
    EmissionsServiceModule.class,
    GeocoderModule.class,
    InteractiveLauncherModule.class,
    RealtimeVehicleServiceModule.class,
    RealtimeVehicleRepositoryModule.class,
    RideHailingServicesModule.class,
    TransitModule.class,
    VehicleParkingServiceModule.class,
    VehicleRentalRepositoryModule.class,
    VehicleRentalServiceModule.class,
    SorlandsbanenNorwayModule.class,
    StopConsolidationServiceModule.class,
    StreetLimitationParametersServiceModule.class,
    WorldEnvelopeServiceModule.class,
  }
)
public interface ConstructApplicationFactory {
  ConfigModel config();
  RaptorConfig<TripSchedule> raptorConfig();
  Graph graph();
  TimetableRepository timetableRepository();
  WorldEnvelopeRepository worldEnvelopeRepository();
  WorldEnvelopeService worldEnvelopeService();
  RealtimeVehicleRepository realtimeVehicleRepository();
  RealtimeVehicleService realtimeVehicleService();
  VehicleRentalRepository vehicleRentalRepository();
  VehicleRentalService vehicleRentalService();
  VehicleParkingRepository vehicleParkingRepository();
  VehicleParkingService vehicleParkingService();
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

  @Nullable
  SorlandsbanenNorwayService enturSorlandsbanenService();

  @Nullable
  LuceneIndex luceneIndex();

  FareService fareService();

  @Component.Builder
  interface Builder {
    @BindsInstance
    Builder configModel(ConfigModel config);

    @BindsInstance
    Builder graph(Graph graph);

    @BindsInstance
    Builder timetableRepository(TimetableRepository timetableRepository);

    @BindsInstance
    Builder graphVisualizer(@Nullable GraphVisualizer graphVisualizer);

    @BindsInstance
    Builder worldEnvelopeRepository(WorldEnvelopeRepository worldEnvelopeRepository);

    @BindsInstance
    Builder stopConsolidationRepository(
      @Nullable StopConsolidationRepository stopConsolidationRepository
    );

    @BindsInstance
    Builder vehicleParkingRepository(VehicleParkingRepository parkingRepository);

    @BindsInstance
    Builder dataImportIssueSummary(DataImportIssueSummary issueSummary);

    @BindsInstance
    Builder emissionsDataModel(EmissionsDataModel emissionsDataModel);

    @BindsInstance
    Builder streetLimitationParameters(StreetLimitationParameters streetLimitationParameters);

    @BindsInstance
    Builder fareService(FareService fareService);

    ConstructApplicationFactory build();
  }
}
