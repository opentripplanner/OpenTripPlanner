package org.opentripplanner.standalone.configure;

import dagger.BindsInstance;
import dagger.Component;
import graphql.schema.GraphQLSchema;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.configure.GtfsSchema;
import org.opentripplanner.apis.gtfs.configure.SchemaModule;
import org.opentripplanner.apis.transmodel.configure.TransmodelSchema;
import org.opentripplanner.apis.transmodel.configure.TransmodelSchemaModule;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.configure.EmissionServiceModule;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayRepository;
import org.opentripplanner.ext.empiricaldelay.configure.EmpiricalDelayServiceModule;
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
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.routing.linking.configure.VertexLinkerRoutingModule;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.routing.via.configure.ViaModule;
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
import org.opentripplanner.updater.trip.TimetableSnapshotManager;
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
    EmissionServiceModule.class,
    EmpiricalDelayServiceModule.class,
    GeocoderModule.class,
    InteractiveLauncherModule.class,
    RealtimeVehicleServiceModule.class,
    RealtimeVehicleRepositoryModule.class,
    RideHailingServicesModule.class,
    SchemaModule.class,
    TransmodelSchemaModule.class,
    SorlandsbanenNorwayModule.class,
    StopConsolidationServiceModule.class,
    StreetLimitationParametersServiceModule.class,
    TransitModule.class,
    VehicleParkingServiceModule.class,
    VehicleRentalRepositoryModule.class,
    VehicleRentalServiceModule.class,
    ViaModule.class,
    VertexLinkerRoutingModule.class,
    WorldEnvelopeServiceModule.class,
  }
)
public interface ConstructApplicationFactory {
  ConfigModel config();
  RaptorConfig<TripSchedule> raptorConfig();
  Graph graph();
  VertexLinker vertexLinker();
  TimetableRepository timetableRepository();
  WorldEnvelopeRepository worldEnvelopeRepository();
  WorldEnvelopeService worldEnvelopeService();
  RealtimeVehicleRepository realtimeVehicleRepository();
  RealtimeVehicleService realtimeVehicleService();
  VehicleRentalRepository vehicleRentalRepository();
  VehicleRentalService vehicleRentalService();
  VehicleParkingRepository vehicleParkingRepository();
  VehicleParkingService vehicleParkingService();
  TimetableSnapshotManager timetableSnapshotManager();
  DataImportIssueSummary dataImportIssueSummary();

  @Nullable
  EmissionRepository emissionRepository();

  @Nullable
  EmpiricalDelayRepository empiricalDelayRepository();

  @Nullable
  GraphVisualizer graphVisualizer();

  TransitService transitService();
  OtpServerRequestContext createServerContext();

  MetricsLogging metricsLogging();

  ViaCoordinateTransferFactory viaTransferResolver();

  @Nullable
  StopConsolidationRepository stopConsolidationRepository();

  StreetLimitationParameters streetLimitationParameters();

  @Nullable
  SorlandsbanenNorwayService enturSorlandsbanenService();

  @Nullable
  @GtfsSchema
  GraphQLSchema gtfsSchema();

  @Nullable
  @TransmodelSchema
  GraphQLSchema transmodelSchema();

  @Nullable
  LuceneIndex luceneIndex();

  FareServiceFactory fareServiceFactory();

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
    Builder emissionRepository(EmissionRepository emissionRepository);

    @BindsInstance
    Builder empiricalDelayRepository(EmpiricalDelayRepository empiricalDelayRepository);

    @BindsInstance
    Builder schema(RouteRequest defaultRouteRequest);

    @BindsInstance
    Builder streetLimitationParameters(StreetLimitationParameters streetLimitationParameters);

    @BindsInstance
    Builder fareServiceFactory(FareServiceFactory fareService);

    ConstructApplicationFactory build();
  }
}
