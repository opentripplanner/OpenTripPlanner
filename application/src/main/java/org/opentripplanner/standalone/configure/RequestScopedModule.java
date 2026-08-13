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
import org.opentripplanner.framework.transaction.configure.TransitDomain;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.filterchain.ext.EmissionDecorator;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepositorySnapshot;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.standalone.api.HttpRequestScoped;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepositorySnapshot;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.service.TransitService;

/**
 * Provides the bindings that live inside {@link RequestScopedFactory}. A single {@link
 * TransactionScope} is captured once per request, and every other binding here is derived from
 * that same scope, so they all see a consistent, pinned view of real-time data.
 * <p>
 * Every {@code @Provides} method below <b>must</b> also be annotated {@link HttpRequestScoped}
 * (enforced by {@link RequestScopedModuleTest}). Without it, Dagger treats the binding as
 * unscoped and re-invokes the provider — constructing a brand-new instance — every single time
 * something depends on that type, even within the same {@link RequestScopedFactory} instance.
 * That matters because {@link org.opentripplanner.standalone.server.DaggerToJerseyBridge}
 * registers a separate HK2 accessor lambda for every binding here, and different resources
 * handling the same HTTP request pull from those lambdas lazily, at different times. An unscoped
 * binding would let two resources in one request each trigger a fresh call and get two different
 * instances — e.g. two {@link TransitService}s each wrapping a different {@code
 * TimetableRepositorySnapshot} if real-time data changed in between. {@code @HttpRequestScoped}
 * is what turns "derived from the same scope" into an actual guarantee: one instance per request,
 * shared by every consumer, instead of silently rebuilt on each lookup.
 */
@Module
public class RequestScopedModule {

  /**
   * The request scope is captured from the transit domain's registry only: the street domain's
   * registry holds no transactional repositories yet. When street repositories are migrated onto
   * the transaction framework, requests will need one scope per registry.
   */
  @Provides
  @HttpRequestScoped
  static TransactionScope transactionScope(@TransitDomain RepositoryRegistry repositoryRegistry) {
    return repositoryRegistry.scope();
  }

  @Provides
  @HttpRequestScoped
  static TransitService transitService(
    TransitRepository transitRepository,
    RepositoryHandle<TimetableRepositorySnapshot, TimetableRepository> timetableRepositoryHandle,
    TransactionScope transactionScope
  ) {
    var timetableSnapshot = timetableRepositoryHandle.repositorySnapshot(transactionScope);
    return new DefaultTransitService(transitRepository, timetableSnapshot);
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
  static OtpServerRequestContext serverRequestContext(
    RouterConfig routerConfig,
    DebugUiConfig debugUiConfig,
    RaptorConfig<TripSchedule> raptorConfig,
    Graph graph,
    LinkingContextFactory linkingContextFactory,
    VertexLinker vertexLinker,
    TransactionScope transactionScope,
    TransitService transitService,
    TransitAlertService transitAlertService,
    RouteRequest defaultRequest,
    VectorTileConfig vectorTileConfig,
    GtfsApiParameters gtfsApiConfig,
    TransmodelAPIParameters transmodelAPIParameters,
    OjpApiParameters ojpApiParameters,
    TriasApiParameters triasApiParameters,
    RegularTransferService transferService,
    WorldEnvelopeService worldEnvelopeService,
    RepositoryHandle<
      RealtimeVehicleRepositorySnapshot,
      RealtimeVehicleRepository
    > realtimeVehicleRepositoryHandle,
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
    @Nullable LuceneIndex luceneIndex,
    FareService fareService
  ) {
    var transitRoutingConfig = routerConfig.transitTuningConfig();
    var flexParameters = routerConfig.flexParameters();

    var realtimeVehicleSnapshot = realtimeVehicleRepositoryHandle.repositorySnapshot(
      transactionScope
    );

    return new DefaultServerRequestContext(
      debugUiConfig,
      fareService,
      flexParameters,
      graph,
      linkingContextFactory,
      Metrics.globalRegistry,
      ojpApiParameters,
      raptorConfig,
      realtimeVehicleSnapshot,
      rideHailingServices,
      defaultRequest,
      streetLimitationParametersService,
      transferService,
      transactionScope,
      transitRoutingConfig,
      transitService,
      transitAlertService,
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
