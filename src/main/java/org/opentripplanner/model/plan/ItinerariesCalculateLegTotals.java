package org.opentripplanner.model.plan;

import java.time.Duration;
import java.util.List;

/**
 * Calculate derived itinerary fields
 */
class ItinerariesCalculateLegTotals {

  Duration totalDurationSeconds = Duration.ZERO;
  Duration transitTimeSeconds = Duration.ZERO;
  int nTransitLegs = 0;
  Duration nonTransitTimeSeconds = Duration.ZERO;
  double nonTransitDistanceMeters = 0.0;
  Duration waitingTimeSeconds;
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
    totalDurationSeconds =
      Duration.between(legs.get(0).getStartTime(), legs.get(legs.size() - 1).getEndTime());

    for (Leg leg : legs) {
      Duration dt = leg.getDuration();

      if (leg.isTransitLeg()) {
        transitTimeSeconds = transitTimeSeconds.plus(dt);
        if (!leg.isInterlinedWithPreviousLeg()) {
          ++nTransitLegs;
        }
      } else if (leg.isStreetLeg()) {
        nonTransitTimeSeconds = nonTransitTimeSeconds.plus(dt);
        nonTransitDistanceMeters += leg.getDistanceMeters();
      }
      if (!leg.isWalkingLeg()) {
        walkOnly = false;
      }
      if (!leg.isStreetLeg()) {
        this.streetOnly = false;
      }
      if (leg.getElevationGained() != null && leg.getElevationLost() != null) {
        this.totalElevationGained += leg.getElevationGained();
        this.totalElevationLost += leg.getElevationLost();
      }
    }
    this.waitingTimeSeconds =
      totalDurationSeconds.minus(transitTimeSeconds.plus(nonTransitTimeSeconds));
  }
}
