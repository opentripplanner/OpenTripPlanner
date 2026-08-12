package org.opentripplanner.standalone.configure;

import dagger.Module;
import dagger.Provides;
import graphql.schema.GraphQLSchema;
import io.micrometer.core.instrument.Metrics;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.GtfsApiParameters;
import org.opentripplanner.apis.gtfs.GtfsGraphQLRequestContext;
import org.opentripplanner.apis.gtfs.configure.GtfsSchema;
import org.opentripplanner.apis.transmodel.TransmodelAPIParameters;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLSchema;
import org.opentripplanner.apis.transmodel.TransmodelRequestContext;
import org.opentripplanner.apis.transmodel.configure.TransmodelSchema;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayService;
import org.opentripplanner.ext.flex.FlexParameters;
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
import org.opentripplanner.place.NearbyPlaceFinder;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StraightLineNearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StreetNearbyStopFinder;
import org.opentripplanner.place.placefinder.StreetNearbyPlaceFinder;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.filterchain.ext.EmissionDecorator;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.service.DefaultRoutingService;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepositorySnapshot;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.standalone.api.HttpRequestScoped;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.street.graph.Graph;
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
    TransitAlertService transitAlertService,
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
      transitAlertService,
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
      // transitRoutingConfig implements 2 roles; hence the repetition below
      transitRoutingConfig,
      transitRoutingConfig
    );
  }

  /**
   * Pre-assembled request context for the Transmodel API's GraphQL data fetchers.
   */
  @Provides
  @HttpRequestScoped
  static TransmodelRequestContext transmodelRequestContext(
    RoutingService routingService,
    TransitService transitService,
    TransitAlertService transitAlertService,
    @Nullable EmpiricalDelayService empiricalDelayService,
    RouteRequest defaultRouteRequest,
    VehicleRentalService vehicleRentalService,
    VehicleParkingService vehicleParkingService,
    Graph graph,
    RegularTransferService transferService,
    StreetDetailsService streetDetailsService,
    LinkingContextFactory linkingContextFactory,
    StreetLimitationParametersService streetLimitationParametersService
  ) {
    return new TransmodelRequestContext(
      routingService,
      transitService,
      transitAlertService,
      empiricalDelayService,
      defaultRouteRequest,
      vehicleRentalService,
      vehicleParkingService,
      graph,
      transferService,
      streetDetailsService,
      linkingContextFactory,
      streetLimitationParametersService
    );
  }

  /**
   * Pre-assembled request context for the GTFS API's GraphQL data fetchers.
   */
  @Provides
  @HttpRequestScoped
  static GtfsGraphQLRequestContext graphQLRequestContext(
    RoutingService routingService,
    TransitService transitService,
    TransitAlertService transitAlertService,
    RegularTransferService transferService,
    FareService fareService,
    VehicleRentalService vehicleRentalService,
    VehicleParkingService vehicleParkingService,
    RepositoryHandle<
      RealtimeVehicleRepositorySnapshot,
      RealtimeVehicleRepository
    > realtimeVehicleRepositoryHandle,
    TransactionScope transactionScope,
    @Nullable @GtfsSchema GraphQLSchema gtfsSchema,
    Graph graph,
    LinkingContextFactory linkingContextFactory,
    RouteRequest defaultRouteRequest
  ) {
    var realtimeVehicleSnapshot = realtimeVehicleRepositoryHandle.repositorySnapshot(
      transactionScope
    );
    RealtimeVehicleService realtimeVehicleService = new DefaultRealtimeVehicleService(
      realtimeVehicleSnapshot,
      transitService
    );
    NearbyPlaceFinder nearbyPlaceFinder = new StreetNearbyPlaceFinder(linkingContextFactory);
    NearbyStopFinder nearbyStopFinder = graph.hasStreets
      ? StreetNearbyStopFinder.of(linkingContextFactory).build()
      : new StraightLineNearbyStopFinder(transitService::findRegularStopsByBoundingBox);

    return new GtfsGraphQLRequestContext(
      routingService,
      transitService,
      transitAlertService,
      transferService,
      fareService,
      vehicleRentalService,
      vehicleParkingService,
      realtimeVehicleService,
      gtfsSchema,
      nearbyPlaceFinder,
      nearbyStopFinder,
      defaultRouteRequest
    );
  }
}
