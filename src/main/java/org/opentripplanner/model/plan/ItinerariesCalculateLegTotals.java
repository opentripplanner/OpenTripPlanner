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
  Duration nonTransitDuration = Duration.ZERO;
  double nonTransitDistanceMeters = 0.0;
  Duration walkingDuration;
  boolean walkOnly = true;
  boolean streetOnly = true;
  double totalElevationGained = 0.0;
  double totalElevationLost = 0.0;

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
    totalDuration =
      Duration.between(legs.get(0).getStartTime(), legs.get(legs.size() - 1).getEndTime());

    for (Leg leg : legs) {
      Duration dt = leg.getDuration();

      if (leg.isTransitLeg()) {
        transitDuration = transitDuration.plus(dt);
        if (!leg.isInterlinedWithPreviousLeg()) {
          ++nTransitLegs;
        }
      } else if (leg.isStreetLeg()) {
        nonTransitDuration = nonTransitDuration.plus(dt);
        nonTransitDistanceMeters += leg.getDistanceMeters();
      }
      if (!leg.isWalkingLeg()) {
        walkOnly = false;
      }
      if (!leg.isStreetLeg()) {
        this.streetOnly = false;
      }
      if (leg.getElevationProfile() != null) {
        var p = leg.getElevationProfile();
        this.totalElevationGained += p.elevationGained();
        this.totalElevationLost += p.elevationLost();
      }
    }
    this.walkingDuration = totalDuration.minus(transitDuration).minus(nonTransitDuration);
  }
}
