package org.opentripplanner.standalone.configure;

import dagger.Module;
import dagger.Provides;
import graphql.schema.GraphQLSchema;
import io.micrometer.core.instrument.Metrics;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.configure.GtfsSchema;
import org.opentripplanner.apis.transmodel.configure.TransmodelSchema;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayService;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.interactivelauncher.api.LauncherRequestDecorator;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.ext.sorlandsbanen.SorlandsbanenNorwayService;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.framework.transaction.RepositoryRegistry;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.api.TransactionScope;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.filterchain.ext.EmissionDecorator;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.standalone.api.HttpRequestScoped;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.repository.MutableTimetableSnapshot;
import org.opentripplanner.transit.repository.ReadOnlyTimetableSnapshot;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.service.TransitService;

/**
 * Provides the bindings that live inside {@link RequestScopedFactory}. A single {@link
 * TransactionScope} is captured once per request, and every other binding here is derived from
 * that same scope, so they all see a consistent, pinned view of real-time data.
 */
@Module
public class RequestScopedModule {

  @Provides
  @HttpRequestScoped
  static TransactionScope transactionScope(RepositoryRegistry repositoryRegistry) {
    return repositoryRegistry.scope();
  }

  @Provides
  @HttpRequestScoped
  static TransitService transitService(
    TransitRepository transitRepository,
    RepositoryHandle<ReadOnlyTimetableSnapshot, MutableTimetableSnapshot> timetableRepositoryHandle,
    TransactionScope transactionScope
  ) {
    var timetableSnapshot = timetableRepositoryHandle.repositorySnapshot(transactionScope);
    return new DefaultTransitService(transitRepository, timetableSnapshot);
  }

  @Provides
  @HttpRequestScoped
  static OtpServerRequestContext serverRequestContext(
    RouterConfig routerConfig,
    DebugUiConfig debugUiConfig,
    RaptorConfig<TripSchedule> raptorConfig,
    Graph graph,
    LinkingContextFactory linkingContextFactory,
    VertexLinker vertexLinker,
    TransactionScope transactionScope,
    TransitService transitService,
    RegularTransferService transferService,
    WorldEnvelopeService worldEnvelopeService,
    RealtimeVehicleRepository realtimeVehicleRepository,
    VehicleRentalService vehicleRentalService,
    VehicleParkingService vehicleParkingService,
    List<RideHailingService> rideHailingServices,
    ViaCoordinateTransferFactory viaTransferResolver,
    @Nullable CarpoolingService carpoolingService,
    @Nullable DataOverlayParameterBindings dataOverlayParameterBindings,
    @Nullable StopConsolidationService stopConsolidationService,
    StreetLimitationParametersService streetLimitationParametersService,
    @Nullable @EmissionDecorator ItineraryDecorator emissionItineraryDecorator,
    StreetDetailsService streetDetailsService,
    @Nullable @GtfsSchema GraphQLSchema gtfsSchema,
    @Nullable @TransmodelSchema GraphQLSchema transmodelSchema,
    @Nullable EmpiricalDelayService empiricalDelayService,
    @Nullable SorlandsbanenNorwayService sorlandsbanenService,
    LauncherRequestDecorator launcherRequestDecorator,
    @Nullable LuceneIndex luceneIndex,
    FareService fareService
  ) {
    var defaultRequest = launcherRequestDecorator.intercept(routerConfig.routingRequestDefaults());

    var transitRoutingConfig = routerConfig.transitTuningConfig();
    var triasApiParameters = routerConfig.triasApiParameters();
    var ojpApiParameters = routerConfig.ojpApiParameters();
    var gtfsApiConfig = routerConfig.gtfsApiParameters();
    var vectorTileConfig = routerConfig.vectorTileConfig();
    var flexParameters = routerConfig.flexParameters();
    var transmodelAPIParameters = routerConfig.transmodelApi();

    return new DefaultServerRequestContext(
      debugUiConfig,
      fareService,
      flexParameters,
      graph,
      linkingContextFactory,
      Metrics.globalRegistry,
      ojpApiParameters,
      raptorConfig,
      realtimeVehicleRepository,
      rideHailingServices,
      defaultRequest,
      streetLimitationParametersService,
      transferService,
      transactionScope,
      transitRoutingConfig,
      transitService,
      triasApiParameters,
      gtfsApiConfig,
      vectorTileConfig,
      vehicleParkingService,
      vehicleRentalService,
      vertexLinker,
      viaTransferResolver,
      worldEnvelopeService,
      // Optional Sandbox services
      carpoolingService,
      dataOverlayParameterBindings,
      emissionItineraryDecorator,
      streetDetailsService,
      empiricalDelayService,
      luceneIndex,
      gtfsSchema,
      transmodelSchema,
      sorlandsbanenService,
      stopConsolidationService,
      transmodelAPIParameters
    );
  }
}
