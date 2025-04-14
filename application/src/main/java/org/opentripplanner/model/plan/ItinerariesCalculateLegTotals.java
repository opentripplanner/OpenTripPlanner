package org.opentripplanner.model.plan;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.model.plan.leg.UnknownPathLeg;

/**
 * Calculate derived itinerary fields
 */
class ItinerariesCalculateLegTotals {

  Duration totalDuration = Duration.ZERO;
  Duration transitDuration = Duration.ZERO;
  int nTransitLegs = 0;
  Duration onStreetDuration = Duration.ZERO;
  double onStreetDistanceMeters = 0.0;
  Duration walkDuration = Duration.ZERO;
  double walkDistanceMeters = 0.0;
  Duration waitingDuration = Duration.ZERO;
  boolean walkOnly = true;
  boolean streetOnly = true;
  double elevationGained_m = 0.0;
  double elevationLost_m = 0.0;

  public ItinerariesCalculateLegTotals(List<Leg> legs) {
    if (legs.isEmpty()) {
      return;
    }
    calculate(legs);
  }

  int transfers() {
    return nTransitLegs == 0 ? 0 : nTransitLegs - 1;
  }

  private void calculate(List<Leg> legs) {
    totalDuration = Duration.between(legs.getFirst().startTime(), legs.getLast().endTime());

    for (Leg leg : legs) {
      Duration dt = leg.duration();

      if (leg.isTransitLeg()) {
        transitDuration = transitDuration.plus(dt);
        if (!leg.isInterlinedWithPreviousLeg()) {
          ++nTransitLegs;
        }
      } else if (leg.isStreetLeg()) {
        onStreetDuration = onStreetDuration.plus(dt);
        onStreetDistanceMeters += leg.distanceMeters();

        if (leg.isWalkingLeg()) {
          walkDuration = walkDuration.plus(leg.duration());
          walkDistanceMeters = walkDistanceMeters + leg.distanceMeters();
        }
      } else if (leg instanceof UnknownPathLeg unknownPathLeg) {
        nTransitLegs += unknownPathLeg.getNumberOfTransfers() + 1;
      }

      if (!leg.isWalkingLeg()) {
        walkOnly = false;
      }

      if (!leg.isStreetLeg()) {
        this.streetOnly = false;
      }

      if (leg.elevationProfile() != null) {
        var p = leg.elevationProfile();
        this.elevationGained_m += p.elevationGained();
        this.elevationLost_m += p.elevationLost();
      }
    }
    this.waitingDuration = totalDuration.minus(transitDuration).minus(onStreetDuration);
  }
}
