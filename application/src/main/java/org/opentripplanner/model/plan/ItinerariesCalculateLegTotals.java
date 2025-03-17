package org.opentripplanner.model.plan;

import java.time.Duration;
import java.util.List;

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
    totalDuration = Duration.between(legs.getFirst().getStartTime(), legs.getLast().getEndTime());

    for (Leg leg : legs) {
      Duration dt = leg.getDuration();

      if (leg.isTransitLeg()) {
        transitDuration = transitDuration.plus(dt);
        if (!leg.isInterlinedWithPreviousLeg()) {
          ++nTransitLegs;
        }
      } else if (leg.isStreetLeg()) {
        onStreetDuration = onStreetDuration.plus(dt);
        onStreetDistanceMeters += leg.getDistanceMeters();

        if (leg.isWalkingLeg()) {
          walkDuration = walkDuration.plus(leg.getDuration());
          walkDistanceMeters = walkDistanceMeters + leg.getDistanceMeters();
        }
      } else if (leg instanceof UnknownTransitPathLeg unknownTransitPathLeg) {
        nTransitLegs += unknownTransitPathLeg.getNumberOfTransfers() + 1;
      }

      if (!leg.isWalkingLeg()) {
        walkOnly = false;
      }

      if (!leg.isStreetLeg()) {
        this.streetOnly = false;
      }

      if (leg.getElevationProfile() != null) {
        var p = leg.getElevationProfile();
        this.elevationGained_m += p.elevationGained();
        this.elevationLost_m += p.elevationLost();
      }
    }
    this.waitingDuration = totalDuration.minus(transitDuration).minus(onStreetDuration);
  }
}
