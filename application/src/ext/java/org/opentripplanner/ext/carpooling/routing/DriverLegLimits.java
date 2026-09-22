package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;

/** How long each leg of a driver trip may take once a passenger is inserted. */
public final class DriverLegLimits {

  private DriverLegLimits() {}

  /**
   * One limit per leg: the leg's travel duration plus the smallest deviation budget among the stops
   * after the leg. A detour on a leg delays every later stop, each of which allows at most its own
   * budget, so no feasible detour adds more than the smallest of them. A zero or negative budget
   * leaves the leg its own duration: only stops on the route itself remain.
   * Capped at {@link CarpoolTrip#MAX_TRIP_DURATION} so that a trip with inconsistent geometry
   * cannot demand a multi-hour street search.
   *
   * @param legDurations the travel duration of each leg, from the same routing model the searches use
   */
  public static Duration[] legLimits(CarpoolTrip trip, Duration[] legDurations) {
    var stops = trip.stops();
    int n = stops.size();
    if (legDurations.length != n - 1) {
      throw new IllegalArgumentException(
        "Expected one duration per leg (" + (n - 1) + "), got " + legDurations.length
      );
    }
    var legLimits = new Duration[n - 1];
    var detourAllowance = stops.get(n - 1).getDeviationBudget();
    for (int k = n - 2; k >= 0; k--) {
      detourAllowance = min(detourAllowance, stops.get(k + 1).getDeviationBudget());
      var allowance = detourAllowance.isNegative() ? Duration.ZERO : detourAllowance;
      legLimits[k] = min(legDurations[k].plus(allowance), CarpoolTrip.MAX_TRIP_DURATION);
    }
    return legLimits;
  }

  private static Duration min(Duration a, Duration b) {
    return a.compareTo(b) <= 0 ? a : b;
  }
}
