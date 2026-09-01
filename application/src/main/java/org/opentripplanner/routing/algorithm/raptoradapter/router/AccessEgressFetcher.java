package org.opentripplanner.routing.algorithm.raptoradapter.router;

import static org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType.ACCESS;
import static org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType.EGRESS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess.RoutingStartOnBoardAccess;
import org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess.TripAndServiceDateResolver;
import org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess.TripLocationResolver;
import org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess.TripScheduleIndexResolver;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * This class exposes methods for fetching access and egress legs for a request.
 * An access or egress may be e.g. a walking path to the first transit stop on a route,
 * but could also include other modes such as bicycle, shared mobility, flex or carpooling.
 */
class AccessEgressFetcher {

  private final RouteRequest request;
  private final TransitService transitService;

  @Nullable
  private final DataOverlayParameterBindings dataOverlayParameterBindings;

  private final LinkingContext linkingContext;
  private final List<AccessEgressRouter> accessRouters;
  private final List<AccessEgressRouter> egressRouters;
  private final TripScheduleIndexResolver tripScheduleIndexResolver;
  private final TripLocationResolver tripLocationResolver;

  /**
   * Creates an {@code AccessEgressFetcher} for a single route request.
   *
   * @param linkingContext contains temporary vertices for request locations.
   * @param accessRouters  the access-side routers, pre-filtered by the caller based on the
   *                       request's access mode and any relevant feature flags.
   * @param egressRouters  the egress-side routers, pre-filtered by the caller based on the
   *                       request's egress mode and any relevant feature flags.
   */
  public AccessEgressFetcher(
    RouteRequest request,
    TransitService transitService,
    @Nullable DataOverlayParameterBindings dataOverlayParameterBindings,
    LinkingContext linkingContext,
    List<AccessEgressRouter> accessRouters,
    List<AccessEgressRouter> egressRouters,
    RaptorRoutingRequestTransitData requestTransitDataProvider
  ) {
    this.request = request;
    this.transitService = transitService;
    this.dataOverlayParameterBindings = dataOverlayParameterBindings;
    this.linkingContext = linkingContext;
    this.accessRouters = accessRouters;
    this.egressRouters = egressRouters;
    this.tripScheduleIndexResolver = new TripScheduleIndexResolver(requestTransitDataProvider);
    this.tripLocationResolver = new TripLocationResolver(transitService);
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

    var routeRequest = accessBuilder.buildRequest();

    var dataOverlayContext = DataOverlayContext.listExtensionRequestContexts(
      routeRequest.preferences().system().dataOverlay(),
      dataOverlayParameterBindings
    );

    var context = new AccessEgressRouterContext(
      routeRequest,
      streetRequest,
      type,
      dataOverlayContext,
      linkingContext
    );

    var routers = type.isAccess() ? accessRouters : egressRouters;
    var results = new ArrayList<RoutingAccessEgress>();
    for (var router : routers) {
      results.addAll(router.route(context));
    }
    return results;
  }
}
