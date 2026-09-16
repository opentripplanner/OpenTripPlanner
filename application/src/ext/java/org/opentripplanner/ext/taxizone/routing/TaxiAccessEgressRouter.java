package org.opentripplanner.ext.taxizone.routing;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.taxizone.TaxiZoneIndex;
import org.opentripplanner.ext.taxizone.model.TaxiZoneLeg;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.lang.Sandbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles taxi zone filtering and decoration for transit access/egress.
 * <ol>
 *   <li>Before the transit search runs, {@link #filterAccessNearbyStops} and
 *   {@link #filterEgressNearbyStops} drop candidate stops whose logical endpoints (the request
 *   origin/destination and the stop) are not covered by a common taxi zone provider, so RAPTOR
 *   never considers a combination that would later be rejected.
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
   * Drops access candidates whose logical endpoints (the request origin and the stop) are not
   * covered by a common taxi zone provider.
   */
  public Collection<NearbyStop> filterAccessNearbyStops(
    TransitService transitService,
    Collection<NearbyStop> nearbyStops,
    WgsCoordinate requestFrom
  ) {
    return nearbyStops
      .stream()
      .filter(nearbyStop ->
        taxiZoneIndex
          .findFirstZone(
            requestFrom,
            transitService.getStopLocation(nearbyStop.stopId).getCoordinate()
          )
          .isPresent()
      )
      .toList();
  }

  /**
   * Drops egress candidates whose logical endpoints (the stop and the request destination) are
   * not covered by a common taxi zone provider.
   */
  public Collection<NearbyStop> filterEgressNearbyStops(
    TransitService transitService,
    Collection<NearbyStop> nearbyStops,
    WgsCoordinate requestTo
  ) {
    return nearbyStops
      .stream()
      .filter(nearbyStop ->
        taxiZoneIndex
          .findFirstZone(
            transitService.getStopLocation(nearbyStop.stopId).getCoordinate(),
            requestTo
          )
          .isPresent()
      )
      .toList();
  }

  /**
   * Decorates the {@link TraverseMode#CAR} leg among an access or egress leg chain with taxi
   * zone information, looking up the zone using the given logical {@code pickup} and
   * {@code dropoff} coordinates (the request origin/destination and the stop), rather than the
   * leg's own local coordinates, which may differ slightly when the access/egress path is a
   * walk-drive-walk chain.
   * <p>
   * Candidates are expected to already have been filtered for zone coverage (see
   * {@link #filterAccessNearbyStops} and {@link #filterEgressNearbyStops}), so a common zone is
   * expected to always exist; if none is found (defensive) the leg is returned unchanged and a
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
