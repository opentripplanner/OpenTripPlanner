package org.opentripplanner.standalone.configure;

import dagger.Module;
import dagger.Provides;
import graphql.schema.GraphQLSchema;
import io.micrometer.core.instrument.Metrics;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.GtfsApiParameters;
import org.opentripplanner.apis.gtfs.configure.GtfsSchema;
import org.opentripplanner.apis.transmodel.TransmodelAPIParameters;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLSchema;
import org.opentripplanner.apis.transmodel.configure.TransmodelSchema;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayService;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.interactivelauncher.api.LauncherRequestDecorator;
import org.opentripplanner.ext.ojp.parameters.OjpApiParameters;
import org.opentripplanner.ext.ojp.parameters.TriasApiParameters;
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
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.service.DefaultRoutingService;
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
import org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.repository.MutableTimetableSnapshot;
import org.opentripplanner.transit.repository.ReadOnlyTimetableSnapshot;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
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
    TimetableRepository timetableRepository,
    RepositoryHandle<ReadOnlyTimetableSnapshot, MutableTimetableSnapshot> timetableRepositoryHandle,
    TransactionScope transactionScope
  ) {
    var timetableSnapshot = timetableRepositoryHandle.repositorySnapshot(transactionScope);
    return new DefaultTransitService(timetableRepository, timetableSnapshot);
  }

  @Provides
  @HttpRequestScoped
  static RouteRequest defaultRouteRequest(
    RouterConfig routerConfig,
    LauncherRequestDecorator launcherRequestDecorator
  ) {
    return launcherRequestDecorator.intercept(routerConfig.routingRequestDefaults());
  }

  @Provides
  @HttpRequestScoped
  static VectorTileConfig vectorTileConfig(RouterConfig routerConfig) {
    return routerConfig.vectorTileConfig();
  }

  @Provides
  @HttpRequestScoped
  static GtfsApiParameters gtfsApiParameters(RouterConfig routerConfig) {
    return routerConfig.gtfsApiParameters();
  }

  @Provides
  @HttpRequestScoped
  static TransmodelAPIParameters transmodelAPIParameters(RouterConfig routerConfig) {
    return routerConfig.transmodelApi();
  }

  @Provides
  @HttpRequestScoped
  static TransmodelGraphQLSchema transmodelGraphQLSchema(
    @Nullable @TransmodelSchema GraphQLSchema transmodelSchema
  ) {
    return new TransmodelGraphQLSchema(transmodelSchema);
  }

  @Provides
  @HttpRequestScoped
  static OjpApiParameters ojpApiParameters(RouterConfig routerConfig) {
    return routerConfig.ojpApiParameters();
  }

  @Provides
  @HttpRequestScoped
  static TriasApiParameters triasApiParameters(RouterConfig routerConfig) {
    return routerConfig.triasApiParameters();
  }

  @Provides
  @HttpRequestScoped
  static FlexParameters flexParameters(RouterConfig routerConfig) {
    return routerConfig.flexParameters();
  }

  @Provides
  @HttpRequestScoped
  static TransitRoutingConfig transitRoutingConfig(RouterConfig routerConfig) {
    return routerConfig.transitTuningConfig();
  }

  @Provides
  @HttpRequestScoped
  static RoutingService routingService(
    TransitService transitService,
    Graph graph,
    RaptorConfig<TripSchedule> raptorConfig,
    StreetLimitationParametersService streetLimitationParametersService,
    VehicleRentalService vehicleRentalService,
    StreetDetailsService streetDetailsService,
    RegularTransferService transferService,
    FlexParameters flexParameters,
    List<RideHailingService> rideHailingServices,
    @Nullable DataOverlayParameterBindings dataOverlayParameterBindings,
    @Nullable SorlandsbanenNorwayService sorlandsbanenService,
    ViaCoordinateTransferFactory viaTransferResolver,
    @Nullable CarpoolingService carpoolingService,
    @Nullable @EmissionDecorator ItineraryDecorator emissionItineraryDecorator,
    @Nullable StopConsolidationService stopConsolidationService,
    LinkingContextFactory linkingContextFactory,
    TransitRoutingConfig transitRoutingConfig
  ) {
    return new DefaultRoutingService(
      transitService,
      graph,
      raptorConfig,
      Metrics.globalRegistry,
      streetLimitationParametersService,
      vehicleRentalService,
      streetDetailsService,
      transferService,
      flexParameters,
      rideHailingServices,
      dataOverlayParameterBindings,
      sorlandsbanenService,
      viaTransferResolver,
      carpoolingService,
      emissionItineraryDecorator,
      stopConsolidationService,
      linkingContextFactory,
      transitRoutingConfig
    );
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
    RouteRequest defaultRequest,
    VectorTileConfig vectorTileConfig,
    GtfsApiParameters gtfsApiConfig,
    TransmodelAPIParameters transmodelAPIParameters,
    OjpApiParameters ojpApiParameters,
    TriasApiParameters triasApiParameters,
    FlexParameters flexParameters,
    TransitRoutingConfig transitRoutingConfig,
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
