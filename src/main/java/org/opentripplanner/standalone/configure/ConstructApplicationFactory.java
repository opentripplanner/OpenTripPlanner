package org.opentripplanner.standalone.configure;

import dagger.BindsInstance;
import dagger.Component;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.ridehailing.configure.RideHailingServicesModule;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehiclepositions.VehiclePositionRepository;
import org.opentripplanner.service.vehiclepositions.VehiclePositionService;
import org.opentripplanner.service.vehiclepositions.configure.VehiclePositionsRepositoryModule;
import org.opentripplanner.service.vehiclepositions.configure.VehiclePositionsServiceModule;
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
    VehiclePositionsServiceModule.class,
    VehiclePositionsRepositoryModule.class,
    VehicleRentalServiceModule.class,
    VehicleRentalRepositoryModule.class,
    ConstructApplicationModule.class,
    RideHailingServicesModule.class,
  }
)
public interface ConstructApplicationFactory {
  ConfigModel config();
  RaptorConfig<TripSchedule> raptorConfig();
  Graph graph();
  TransitModel transitModel();
  WorldEnvelopeRepository worldEnvelopeRepository();
  WorldEnvelopeService worldEnvelopeService();
  VehiclePositionRepository vehiclePositionRepository();
  VehiclePositionService vehiclePositionService();
  VehicleRentalRepository vehicleRentalRepository();
  VehicleRentalService vehicleRentalService();
  DataImportIssueSummary dataImportIssueSummary();

  @Nullable
  GraphVisualizer graphVisualizer();

  TransitService transitService();
  OtpServerRequestContext createServerContext();

  MetricsLogging metricsLogging();

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
    Builder dataImportIssueSummary(DataImportIssueSummary issueSummary);

    ConstructApplicationFactory build();
  }
}
