package org.opentripplanner.standalone.server;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.ext.emissions.EmissionsService;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.ext.sorlandsbanen.SorlandsbanenNorwayService;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.inspector.raster.TileRendererManager;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.service.DefaultRoutingService;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.standalone.api.HttpRequestScoped;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.TransitService;

@HttpRequestScoped
public class DefaultServerRequestContext implements OtpServerRequestContext {

  private final List<RideHailingService> rideHailingServices;
  private final Graph graph;
  private final TransitService transitService;
  private final TransitRoutingConfig transitRoutingConfig;
  private final RouteRequest routeRequestDefaults;
  private final MeterRegistry meterRegistry;
  private final RaptorConfig<TripSchedule> raptorConfig;
  private final TileRendererManager tileRendererManager;
  private final VectorTileConfig vectorTileConfig;
  private final FlexParameters flexParameters;
  private final TraverseVisitor traverseVisitor;
  private final WorldEnvelopeService worldEnvelopeService;
  private final RealtimeVehicleService realtimeVehicleService;
  private final VehicleRentalService vehicleRentalService;
  private final VehicleParkingService vehicleParkingService;
  private final EmissionsService emissionsService;

  @Nullable
  private final SorlandsbanenNorwayService sorlandsbanenService;

  private final StopConsolidationService stopConsolidationService;
  private final StreetLimitationParametersService streetLimitationParametersService;
  private final LuceneIndex luceneIndex;
  private final DebugUiConfig debugUiConfig;

  private RouteRequest defaultRouteRequestWithTimeSet = null;

  /**
   * Make sure all mutable components are copied/cloned before calling this constructor.
   */
  private DefaultServerRequestContext(
    Graph graph,
    TransitService transitService,
    TransitRoutingConfig transitRoutingConfig,
    RouteRequest routeRequestDefaults,
    MeterRegistry meterRegistry,
    RaptorConfig<TripSchedule> raptorConfig,
    TileRendererManager tileRendererManager,
    VectorTileConfig vectorTileConfig,
    WorldEnvelopeService worldEnvelopeService,
    RealtimeVehicleService realtimeVehicleService,
    VehicleRentalService vehicleRentalService,
    VehicleParkingService vehicleParkingService,
    @Nullable EmissionsService emissionsService,
    @Nullable SorlandsbanenNorwayService sorlandsbanenService,
    List<RideHailingService> rideHailingServices,
    @Nullable StopConsolidationService stopConsolidationService,
    StreetLimitationParametersService streetLimitationParametersService,
    FlexParameters flexParameters,
    @Nullable TraverseVisitor traverseVisitor,
    @Nullable LuceneIndex luceneIndex,
    DebugUiConfig debugUiConfig
  ) {
    this.graph = graph;
    this.transitService = transitService;
    this.transitRoutingConfig = transitRoutingConfig;
    this.meterRegistry = meterRegistry;
    this.raptorConfig = raptorConfig;
    this.tileRendererManager = tileRendererManager;
    this.vectorTileConfig = vectorTileConfig;
    this.vehicleRentalService = vehicleRentalService;
    this.vehicleParkingService = vehicleParkingService;
    this.flexParameters = flexParameters;
    this.traverseVisitor = traverseVisitor;
    this.routeRequestDefaults = routeRequestDefaults;
    this.worldEnvelopeService = worldEnvelopeService;
    this.realtimeVehicleService = realtimeVehicleService;
    this.rideHailingServices = rideHailingServices;
    this.emissionsService = emissionsService;
    this.sorlandsbanenService = sorlandsbanenService;
    this.stopConsolidationService = stopConsolidationService;
    this.streetLimitationParametersService = streetLimitationParametersService;
    this.luceneIndex = luceneIndex;
    this.debugUiConfig = debugUiConfig;
  }

  /**
   * Create a server context valid for one http request only!
   */
  public static DefaultServerRequestContext create(
    TransitRoutingConfig transitRoutingConfig,
    RouteRequest routeRequestDefaults,
    RaptorConfig<TripSchedule> raptorConfig,
    Graph graph,
    TransitService transitService,
    MeterRegistry meterRegistry,
    VectorTileConfig vectorTileConfig,
    WorldEnvelopeService worldEnvelopeService,
    RealtimeVehicleService realtimeVehicleService,
    VehicleRentalService vehicleRentalService,
    VehicleParkingService vehicleParkingService,
    @Nullable EmissionsService emissionsService,
    @Nullable SorlandsbanenNorwayService sorlandsbanenService,
    FlexParameters flexParameters,
    List<RideHailingService> rideHailingServices,
    @Nullable StopConsolidationService stopConsolidationService,
    StreetLimitationParametersService streetLimitationParametersService,
    @Nullable TraverseVisitor traverseVisitor,
    @Nullable LuceneIndex luceneIndex,
    DebugUiConfig debugUiConfig
  ) {
    return new DefaultServerRequestContext(
      graph,
      transitService,
      transitRoutingConfig,
      routeRequestDefaults,
      meterRegistry,
      raptorConfig,
      new TileRendererManager(graph, routeRequestDefaults.preferences()),
      vectorTileConfig,
      worldEnvelopeService,
      realtimeVehicleService,
      vehicleRentalService,
      vehicleParkingService,
      emissionsService,
      sorlandsbanenService,
      rideHailingServices,
      stopConsolidationService,
      streetLimitationParametersService,
      flexParameters,
      traverseVisitor,
      luceneIndex,
      debugUiConfig
    );
  }

  @Override
  public RouteRequest defaultRouteRequest() {
    // Lazy initialize request-scoped request to avoid doing this when not needed
    if (defaultRouteRequestWithTimeSet == null) {
      defaultRouteRequestWithTimeSet = routeRequestDefaults.copyWithDateTimeNow();
    }
    return defaultRouteRequestWithTimeSet;
  }

  /**
   * Return the default routing request locale(without cloning the request).
   */
  @Override
  public Locale defaultLocale() {
    return routeRequestDefaults.locale();
  }

  @Override
  public RaptorConfig<TripSchedule> raptorConfig() {
    return raptorConfig;
  }

  @Override
  public Graph graph() {
    return graph;
  }

  @Override
  public TransitService transitService() {
    return transitService;
  }

  @Override
  public RoutingService routingService() {
    return new DefaultRoutingService(this);
  }

  @Override
  public WorldEnvelopeService worldEnvelopeService() {
    return worldEnvelopeService;
  }

  @Override
  public RealtimeVehicleService realtimeVehicleService() {
    return realtimeVehicleService;
  }

  @Override
  public VehicleRentalService vehicleRentalService() {
    return vehicleRentalService;
  }

  @Override
  public VehicleParkingService vehicleParkingService() {
    return vehicleParkingService;
  }

  @Override
  public TransitTuningParameters transitTuningParameters() {
    return transitRoutingConfig;
  }

  @Override
  public RaptorTuningParameters raptorTuningParameters() {
    return transitRoutingConfig;
  }

  @Override
  public List<RideHailingService> rideHailingServices() {
    return rideHailingServices;
  }

  @Override
  public StopConsolidationService stopConsolidationService() {
    return stopConsolidationService;
  }

  @Override
  public StreetLimitationParametersService streetLimitationParametersService() {
    return streetLimitationParametersService;
  }

  @Override
  public MeterRegistry meterRegistry() {
    return meterRegistry;
  }

  @Override
  public TileRendererManager tileRendererManager() {
    return tileRendererManager;
  }

  @Override
  public TraverseVisitor traverseVisitor() {
    return traverseVisitor;
  }

  @Override
  public FlexParameters flexParameters() {
    return flexParameters;
  }

  @Override
  public VectorTileConfig vectorTileConfig() {
    return vectorTileConfig;
  }

  @Override
  public DebugUiConfig debugUiConfig() {
    return debugUiConfig;
  }

  @Nullable
  @Override
  public LuceneIndex lucenceIndex() {
    return luceneIndex;
  }

  @Override
  public EmissionsService emissionsService() {
    return emissionsService;
  }

  @Nullable
  public SorlandsbanenNorwayService sorlandsbanenService() {
    return sorlandsbanenService;
  }
}
