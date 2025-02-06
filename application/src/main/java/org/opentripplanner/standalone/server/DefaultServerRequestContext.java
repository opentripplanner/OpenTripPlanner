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

  // Keep sort order: Main services, optional services and writable/none final fields
  //                  All 3 sections is sorted alphabetically.

  private final DebugUiConfig debugUiConfig;
  private final FlexParameters flexParameters;
  private final Graph graph;
  private final MeterRegistry meterRegistry;
  private final RaptorConfig<TripSchedule> raptorConfig;
  private final RealtimeVehicleService realtimeVehicleService;
  private final List<RideHailingService> rideHailingServices;
  private final RouteRequest routeRequestDefaults;
  private final StreetLimitationParametersService streetLimitationParametersService;
  private final TransitRoutingConfig transitRoutingConfig;
  private final TransitService transitService;
  private final VectorTileConfig vectorTileConfig;
  private final VehicleParkingService vehicleParkingService;
  private final VehicleRentalService vehicleRentalService;
  private final WorldEnvelopeService worldEnvelopeService;

  /* Optional fields */

  @Nullable
  private final EmissionsService emissionsService;

  @Nullable
  private final LuceneIndex luceneIndex;

  @Nullable
  private final SorlandsbanenNorwayService sorlandsbanenService;

  @Nullable
  private final StopConsolidationService stopConsolidationService;

  @Nullable
  private final TraverseVisitor traverseVisitor;

  /* Lazy initialized fields */

  private RouteRequest defaultRouteRequestWithTimeSet = null;

  /**
   * Create a server context valid for one http request only!
   * Make sure all mutable components are copied/cloned before calling this constructor.
   */
  public DefaultServerRequestContext(
    // Keep the same order as in the field declaration
    DebugUiConfig debugUiConfig,
    FlexParameters flexParameters,
    Graph graph,
    MeterRegistry meterRegistry,
    RaptorConfig<TripSchedule> raptorConfig,
    RealtimeVehicleService realtimeVehicleService,
    List<RideHailingService> rideHailingServices,
    RouteRequest routeRequestDefaults,
    StreetLimitationParametersService streetLimitationParametersService,
    TransitRoutingConfig transitRoutingConfig,
    TransitService transitService,
    VectorTileConfig vectorTileConfig,
    VehicleParkingService vehicleParkingService,
    VehicleRentalService vehicleRentalService,
    WorldEnvelopeService worldEnvelopeService,
    @Nullable EmissionsService emissionsService,
    @Nullable LuceneIndex luceneIndex,
    @Nullable SorlandsbanenNorwayService sorlandsbanenService,
    @Nullable StopConsolidationService stopConsolidationService,
    @Nullable TraverseVisitor traverseVisitor
  ) {
    this.debugUiConfig = debugUiConfig;
    this.flexParameters = flexParameters;
    this.graph = graph;
    this.meterRegistry = meterRegistry;
    this.raptorConfig = raptorConfig;
    this.realtimeVehicleService = realtimeVehicleService;
    this.rideHailingServices = rideHailingServices;
    this.routeRequestDefaults = routeRequestDefaults;
    this.streetLimitationParametersService = streetLimitationParametersService;
    this.transitRoutingConfig = transitRoutingConfig;
    this.transitService = transitService;
    this.vectorTileConfig = vectorTileConfig;
    this.vehicleParkingService = vehicleParkingService;
    this.vehicleRentalService = vehicleRentalService;
    this.worldEnvelopeService = worldEnvelopeService;

    // Optional fields
    this.emissionsService = emissionsService;
    this.luceneIndex = luceneIndex;
    this.sorlandsbanenService = sorlandsbanenService;
    this.stopConsolidationService = stopConsolidationService;
    this.traverseVisitor = traverseVisitor;
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
