package org.opentripplanner.routing.algorithm.raptoradapter.router;

import static org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType.ACCESS;
import static org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType.EGRESS;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.ridehailing.RideHailingAccessShifter;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.FlexAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transit.service.TransitServiceResolver;

/**
 * This class exposes methods for fetching access and egress legs for a request.
 * An access or egress may be e.g. a walking path to the first transit stop on a route,
 * but could also include other modes such as bicycle, shared mobility, flex or carpooling.
 */
class AccessEgressFetcher {

  private final RouteRequest request;
  private final OtpServerRequestContext serverContext;
  private final ZonedDateTime transitSearchTimeZero;
  private final AdditionalSearchDays additionalSearchDays;
  private final LinkingContext linkingContext;
  private final TransitServiceResolver transitServiceResolver;
  private final AccessEgressMapper accessEgressMapper;
  private final CarpoolingService carpoolingService;

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
    OtpServerRequestContext serverContext,
    ZonedDateTime transitSearchTimeZero,
    AdditionalSearchDays additionalSearchDays,
    LinkingContext linkingContext,
    CarpoolingService carpoolingService
  ) {
    this.request = request;
    this.serverContext = serverContext;
    this.transitSearchTimeZero = transitSearchTimeZero;
    this.additionalSearchDays = additionalSearchDays;
    this.linkingContext = linkingContext;
    this.carpoolingService = carpoolingService;
    this.transitServiceResolver = new TransitServiceResolver(serverContext.transitService());
    this.accessEgressMapper = new AccessEgressMapper(transitServiceResolver);
  }

  Collection<? extends RoutingAccessEgress> fetchAccess() {
    return fetchAccessEgresses(ACCESS);
  }

  Collection<? extends RoutingAccessEgress> fetchEgress() {
    return fetchAccessEgresses(EGRESS);
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

    var nearbyStops = AccessEgressRouter.findAccessEgresses(
      accessRequest,
      mode,
      serverContext.listExtensionRequestContexts(accessRequest),
      type,
      durationLimit,
      stopCountLimit,
      linkingContext
    );
    var accessEgresses = accessEgressMapper.mapNearbyStops(nearbyStops);
    accessEgresses = timeshiftRideHailing(streetRequest, type, accessEgresses);

    var results = new ArrayList<>(accessEgresses);

    // Special handling of flex accesses
    if (OTPFeature.FlexRouting.isOn() && mode == StreetMode.FLEXIBLE) {
      var flexAccessList = FlexAccessEgressRouter.routeAccessEgress(
        accessRequest,
        serverContext,
        additionalSearchDays,
        serverContext.flexParameters(),
        serverContext.listExtensionRequestContexts(accessRequest),
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
      serverContext.rideHailingServices(),
      request,
      Instant.now()
    );
  }
}
