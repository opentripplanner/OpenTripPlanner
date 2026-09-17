package org.opentripplanner.ext.taxizone.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.taxizone.TaxiZoneIndex;
import org.opentripplanner.ext.taxizone.model.TaxiZone;
import org.opentripplanner.ext.taxizone.model.TaxiZoneLeg;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.lang.Sandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles taxi zone filtering and decoration for transit access/egress.
 * <ol>
 *   <li>Before the transit search runs, {@link #filterNearbyStops} drops candidate stops whose
 *   logical endpoints (the request origin/destination and the stop) are not covered by a common
 *   taxi zone provider, so RAPTOR never considers a combination that would later be rejected.
 *   <li>Once the transit search has picked a surviving candidate and its legs are built,
 *   {@link #decorateAccessEgressLegs} replaces the {@link TraverseMode#CAR} leg with a
 *   {@link TaxiZoneLeg}, using the same logical coordinates.
 * </ol>
 */
@Sandbox
public class TaxiAccessEgressRouter {

  private static final Logger LOG = LoggerFactory.getLogger(TaxiAccessEgressRouter.class);

  private final TaxiZoneIndex taxiZoneIndex;

  public TaxiAccessEgressRouter(TaxiZoneIndex taxiZoneIndex) {
    this.taxiZoneIndex = taxiZoneIndex;
  }

  /**
   * Drops access/egress candidates whose logical endpoints (the request origin/destination and
   * the stop) are not covered by a common taxi zone provider.
   */
  public Collection<NearbyStop> filterNearbyStops(
    TransitService transitService,
    Collection<NearbyStop> nearbyStops,
    AccessEgressType type,
    RouteRequest request
  ) {
    return type.isAccess()
      ? filterNearbyStops(transitService, nearbyStops, request.from().wgsCoordinate())
      : filterNearbyStops(transitService, nearbyStops, request.to().wgsCoordinate());
  }

  private Collection<NearbyStop> filterNearbyStops(
    TransitService transitService,
    Collection<NearbyStop> nearbyStops,
    WgsCoordinate coordinate
  ) {
    List<TaxiZone> zones = taxiZoneIndex.findAllZones(coordinate);
    if (zones.isEmpty()) {
      return List.of();
    }

    List<NearbyStop> result = new ArrayList<>(nearbyStops.size());
    for (NearbyStop nearbyStop : nearbyStops) {
      WgsCoordinate stopCoordinate = transitService
        .getStopLocation(nearbyStop.stopId)
        .getCoordinate();
      for (TaxiZone zone : zones) {
        if (zone.contains(stopCoordinate)) {
          result.add(nearbyStop);
          break;
        }
      }
    }
    return result;
  }

  /**
   * Decorates the {@link TraverseMode#CAR} leg among an access or egress leg chain with taxi
   * zone information, looking up the zone using the given logical {@code pickup} and
   * {@code dropoff} coordinates (the request origin/destination and the stop), rather than the
   * leg's own local coordinates, which may differ slightly when the access/egress path is a
   * walk-drive-walk chain.
   * <p>
   * Candidates are expected to already have been filtered for zone coverage (see
   * {@link #filterNearbyStops}), so a common zone is expected to always exist; if none is found
   * (defensive) the leg is returned unchanged and a
   * warning is logged.
   */
  public List<Leg> decorateAccessEgressLegs(
    List<Leg> legs,
    WgsCoordinate pickup,
    WgsCoordinate dropoff
  ) {
    return legs
      .stream()
      .map(leg -> decorateAccessEgressLeg(leg, pickup, dropoff))
      .toList();
  }

  private Leg decorateAccessEgressLeg(Leg leg, WgsCoordinate pickup, WgsCoordinate dropoff) {
    if (!(leg instanceof StreetLeg streetLeg) || streetLeg.getMode() != TraverseMode.CAR) {
      return leg;
    }
    var taxiZone = taxiZoneIndex.findFirstZone(pickup, dropoff);
    if (taxiZone.isEmpty()) {
      LOG.warn(
        "No taxi zone covers the pre-filtered access/egress leg between {} and {}",
        pickup,
        dropoff
      );
      return leg;
    }
    return new TaxiZoneLeg(streetLeg, taxiZone.get());
  }
}
