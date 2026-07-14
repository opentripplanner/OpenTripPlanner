package org.opentripplanner.routing.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.ext.sorlandsbanen.SorlandsbanenNorwayService;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.time.ZoneIdFallback;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.RequestPreProcessor;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.algorithm.RoutingWorkerRequest;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.ItineraryDecorator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.via.ViaRoutingWorker;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.standalone.config.routerconfig.TransitRoutingConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.tostring.MultiLineToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO VIA: 2022-08-29 javadocs

/**
 * Entry point for requests towards the routing API.
 */
public class DefaultRoutingService implements RoutingService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultRoutingService.class);

  private final TransitService transitService;
  private final Graph graph;
  private final RaptorConfig<TripSchedule> raptorConfig;
  private final MeterRegistry meterRegistry;
  private final StreetLimitationParametersService streetLimitationParametersService;
  private final VehicleRentalService vehicleRentalService;
  private final StreetDetailsService streetDetailsService;
  private final RegularTransferService transferService;
  private final FlexParameters flexParameters;
  private final List<RideHailingService> rideHailingServices;
  private final ViaCoordinateTransferFactory viaTransferResolver;
  private final LinkingContextFactory linkingContextFactory;
  private final TransitRoutingConfig transitRoutingConfig;

  @Nullable
  private final DataOverlayParameterBindings dataOverlayParameterBindings;

  @Nullable
  private final SorlandsbanenNorwayService sorlandsbanenService;

  @Nullable
  private final CarpoolingService carpoolingService;

  @Nullable
  private final ItineraryDecorator emissionItineraryDecorator;

  @Nullable
  private final StopConsolidationService stopConsolidationService;

  private final RequestPreProcessor requestPreProcessor;

  public DefaultRoutingService(
    TransitService transitService,
    Graph graph,
    RaptorConfig<TripSchedule> raptorConfig,
    MeterRegistry meterRegistry,
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
    @Nullable ItineraryDecorator emissionItineraryDecorator,
    @Nullable StopConsolidationService stopConsolidationService,
    LinkingContextFactory linkingContextFactory,
    TransitRoutingConfig transitRoutingConfig
  ) {
    this.transitService = transitService;
    this.graph = graph;
    this.raptorConfig = raptorConfig;
    this.meterRegistry = meterRegistry;
    this.streetLimitationParametersService = streetLimitationParametersService;
    this.vehicleRentalService = vehicleRentalService;
    this.streetDetailsService = streetDetailsService;
    this.transferService = transferService;
    this.flexParameters = flexParameters;
    this.rideHailingServices = rideHailingServices;
    this.dataOverlayParameterBindings = dataOverlayParameterBindings;
    this.sorlandsbanenService = sorlandsbanenService;
    this.viaTransferResolver = viaTransferResolver;
    this.carpoolingService = carpoolingService;
    this.emissionItineraryDecorator = emissionItineraryDecorator;
    this.stopConsolidationService = stopConsolidationService;
    this.linkingContextFactory = linkingContextFactory;
    this.transitRoutingConfig = transitRoutingConfig;

    var timeZone = ZoneIdFallback.zoneId(transitService.getTimeZone());

    this.requestPreProcessor = new RequestPreProcessor(
      transitService,
      transitRoutingConfig,
      timeZone
    );
  }

  @Override
  public RoutingResponse route(RouteRequest request) {
    LOG.debug("Request: {}", request);
    OTPRequestTimeoutException.checkForTimeout();
    request.validateOriginAndDestination();
    var worker = newRoutingWorker(mapRequest(request));
    var response = worker.route();
    logResponse(response);
    return response;
  }

  @Override
  public ViaRoutingResponse route(RouteViaRequest request) {
    LOG.debug("Request: {}", request);
    OTPRequestTimeoutException.checkForTimeout();
    var viaRoutingWorker = new ViaRoutingWorker(request, req ->
      newRoutingWorker(mapRequest(req)).route()
    );
    // TODO: Add output logging here, see route(..) method
    return viaRoutingWorker.route();
  }

  private RoutingWorker newRoutingWorker(RoutingWorkerRequest workerRequest) {
    return new RoutingWorker(
      transitService,
      graph,
      raptorConfig,
      meterRegistry,
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
      transitRoutingConfig,
      workerRequest
    );
  }

  private RoutingWorkerRequest mapRequest(RouteRequest request) {
    return requestPreProcessor.computeRequest(request);
  }

  private void logResponse(RoutingResponse response) {
    if (response.getTripPlan().itineraries.isEmpty() && response.getRoutingErrors().isEmpty()) {
      // We should provide an error if there is no results, this is important for the client so
      // it knows if it can page or abort.
      LOG.warn("The routing result is empty, but there is no errors...");
    }

    if (LOG.isDebugEnabled()) {
      var text = MultiLineToStringBuilder.of("Response")
        .add("NextPage", response.getNextPageCursor())
        .add("PreviousPage", response.getPreviousPageCursor())
        .addColNl(
          "Itineraries",
          response.getTripPlan().itineraries.stream().map(Itinerary::toStr).toList()
        )
        .addColNl("Errors", response.getRoutingErrors())
        .toString();
      LOG.debug(text);
    }
  }
}
