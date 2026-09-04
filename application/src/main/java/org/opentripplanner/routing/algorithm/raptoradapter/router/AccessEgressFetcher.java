package org.opentripplanner.routing.algorithm.raptoradapter.router;

import static org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType.ACCESS;
import static org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType.EGRESS;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.ridehailing.RideHailingAccessShifter;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess.RoutingStartOnBoardAccess;
import org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess.TripAndServiceDateResolver;
import org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess.TripLocationResolver;
import org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess.TripScheduleIndexResolver;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressRouterFactory;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.FlexAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.transit.service.TransitServiceResolver;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * This class exposes methods for fetching access and egress legs for a request.
 * An access or egress may be e.g. a walking path to the first transit stop on a route,
 * but could also include other modes such as bicycle, shared mobility, flex or carpooling.
 */
class AccessEgressFetcher {

  private final RouteRequest request;
  private final TransitService transitService;
  private final Graph graph;
  private final RegularTransferService transferService;
  private final GeofencingZoneService geofencingZoneService;
  private final StreetLimitationParametersService streetLimitationParametersService;
  private final StreetDetailsService streetDetailsService;
  private final FlexParameters flexParameters;
  private final List<RideHailingService> rideHailingServices;

  @Nullable
  private final DataOverlayParameterBindings dataOverlayParameterBindings;

  private final ZonedDateTime transitSearchTimeZero;
  private final AdditionalSearchDays additionalSearchDays;
  private final LinkingContext linkingContext;
  private final TransitServiceResolver transitServiceResolver;
  private final AccessEgressMapper accessEgressMapper;
  private final AccessEgressRouter accessEgressRouter;
  private final CarpoolingService carpoolingService;
  private final TripScheduleIndexResolver tripScheduleIndexResolver;
  private final TripLocationResolver tripLocationResolver;

  /**
   * Creates an {@code AccessEgressFetcher} for a single route request.
   *
   * @param transitSearchTimeZero the point in time all times in seconds are counted from
   * @param additionalSearchDays  extra search days beyond the departure day, required for flex
   *                              routing
   * @param linkingContext        contains temporary vertices for request locations.
   */
  public AccessEgressFetcher(
    RouteRequest request,
    TransitService transitService,
    Graph graph,
    RegularTransferService transferService,
    GeofencingZoneService geofencingZoneService,
    StreetLimitationParametersService streetLimitationParametersService,
    StreetDetailsService streetDetailsService,
    FlexParameters flexParameters,
    List<RideHailingService> rideHailingServices,
    @Nullable DataOverlayParameterBindings dataOverlayParameterBindings,
    ZonedDateTime transitSearchTimeZero,
    AdditionalSearchDays additionalSearchDays,
    LinkingContext linkingContext,
    CarpoolingService carpoolingService,
    RaptorRoutingRequestTransitData requestTransitDataProvider
  ) {
    this.request = request;
    this.transitService = transitService;
    this.graph = graph;
    this.transferService = transferService;
    this.geofencingZoneService = geofencingZoneService;
    this.streetLimitationParametersService = streetLimitationParametersService;
    this.streetDetailsService = streetDetailsService;
    this.flexParameters = flexParameters;
    this.rideHailingServices = rideHailingServices;
    this.dataOverlayParameterBindings = dataOverlayParameterBindings;
    this.transitSearchTimeZero = transitSearchTimeZero;
    this.additionalSearchDays = additionalSearchDays;
    this.linkingContext = linkingContext;
    this.carpoolingService = carpoolingService;
    this.transitServiceResolver = new TransitServiceResolver(transitService);
    this.accessEgressMapper = new AccessEgressMapper(transitServiceResolver);
    this.tripScheduleIndexResolver = new TripScheduleIndexResolver(requestTransitDataProvider);
    this.tripLocationResolver = new TripLocationResolver(transitService);
    this.accessEgressRouter = AccessEgressRouterFactory.create(request);
  }

  Collection<? extends RoutingAccessEgress> fetchAccess() {
    if (request.isStartOnBoardAccessRequest()) {
      return List.of(fetchStartOnBoardAccess());
    }
    return fetchAccessEgresses(ACCESS);
  }

  Collection<? extends RoutingAccessEgress> fetchEgress() {
    return fetchAccessEgresses(EGRESS);
  }

  RoutingStartOnBoardAccess fetchStartOnBoardAccess() {
    var from = request.from();
    var onBoardTripLocation = from != null ? from.tripLocation() : null;
    if (onBoardTripLocation == null) {
      throw new IllegalArgumentException(
        "Cannot fetch start-on-board-access for a request without an on-board trip location"
      );
    }

    var tripAndServiceDate = new TripAndServiceDateResolver(transitService).resolve(
      onBoardTripLocation.tripOnDateReference()
    );
    var aimedDeparture = onBoardTripLocation.aimedDepartureTime();
    Integer aimedDepartureSeconds = aimedDeparture == null
      ? null
      : ServiceDateUtils.secondsSinceStartOfTime(
          ServiceDateUtils.asStartOfService(
            tripAndServiceDate.serviceDate(),
            transitService.getTimeZone()
          ),
          aimedDeparture
        );
    var tripLocation = tripLocationResolver.resolve(
      tripAndServiceDate,
      onBoardTripLocation.stopLocationId(),
      aimedDepartureSeconds
    );

    var tripScheduleIndex = tripScheduleIndexResolver.resolve(tripAndServiceDate, tripLocation);

    return new RoutingStartOnBoardAccess(tripScheduleIndex, tripLocation);
  }

  private Collection<? extends RoutingAccessEgress> fetchAccessEgresses(AccessEgressType type) {
    var streetRequest = type.isAccess() ? request.journey().access() : request.journey().egress();
    StreetMode mode = streetRequest.mode();

    // Prepare access/egress lists
    var accessBuilder = request.copyOf();

    if (type.isAccess()) {
      accessBuilder.withPreferences(p -> {
        p.withBike(b -> b.withRental(r -> r.withAllowArrivingInRentedVehicleAtDestination(false)));
        p.withCar(c -> c.withRental(r -> r.withAllowArrivingInRentedVehicleAtDestination(false)));
        p.withScooter(s ->
          s.withRental(r -> r.withAllowArrivingInRentedVehicleAtDestination(false))
        );
      });
    }

    var accessRequest = accessBuilder.buildRequest();

    var accessEgressPreferences = accessRequest.preferences().street().accessEgress();

    Duration durationLimit = accessEgressPreferences.maxDuration().valueOf(mode);
    int stopCountLimit = accessEgressPreferences.maxStopCountLimit().limitForMode(mode);

    var dataOverlayContext = DataOverlayContext.listExtensionRequestContexts(
      accessRequest.preferences().system().dataOverlay(),
      dataOverlayParameterBindings
    );
    var nearbyStops = accessEgressRouter.findAccessEgresses(
      accessRequest,
      mode,
      dataOverlayContext,
      type,
      durationLimit,
      stopCountLimit,
      linkingContext,
      streetLimitationParametersService,
      geofencingZoneService
    );
    var accessEgresses = accessEgressMapper.mapNearbyStops(nearbyStops);
    accessEgresses = timeshiftRideHailing(streetRequest, type, accessEgresses);

    var results = new ArrayList<>(accessEgresses);

    // Special handling of flex accesses
    if (OTPFeature.FlexRouting.isOn() && mode == StreetMode.FLEXIBLE) {
      var flexAccessList = FlexAccessEgressRouter.routeAccessEgress(
        accessRequest,
        transitService,
        graph,
        transferService,
        geofencingZoneService,
        streetLimitationParametersService,
        streetDetailsService,
        accessEgressRouter,
        additionalSearchDays,
        flexParameters,
        dataOverlayContext,
        type,
        linkingContext
      );

      results.addAll(AccessEgressMapper.mapFlexAccessEgresses(flexAccessList));
    }

    if (OTPFeature.CarPooling.isOn() && mode == StreetMode.CARPOOL) {
      var carpoolAccessEgressList = carpoolingService.routeAccessEgress(
        accessRequest,
        streetRequest,
        type,
        transitServiceResolver,
        transitSearchTimeZero
      );
      results.addAll(carpoolAccessEgressList);
    }

    return results;
  }

  /**
   * Given a list of {@code results} shift the access ones that contain driving so that they only
   * start at the time when the ride hailing vehicle can actually be there to pick up passengers.
   * <p>
   * If there are accesses/egresses with only walking, then they remain unchanged.
   * <p>
   * This method is a good candidate to be moved to the access/egress filter chain when that has
   * been added.
   */
  private List<RoutingAccessEgress> timeshiftRideHailing(
    StreetRequest streetRequest,
    AccessEgressType type,
    List<RoutingAccessEgress> accessEgressList
  ) {
    if (streetRequest.mode() != StreetMode.CAR_HAILING) {
      return accessEgressList;
    }
    return RideHailingAccessShifter.shiftAccesses(
      type.isAccess(),
      accessEgressList,
      rideHailingServices,
      request,
      Instant.now()
    );
  }
}
