package org.opentripplanner.model.plan.itineraryreference;

import java.util.Objects;
import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.street.model.VehicleRoutingOptimizeType;

/**
 * How a client wants to ride a rented bike or scooter.
 *
 * @param speed The speed in meters per second.
 * @param reluctance The cost factor for time spent riding.
 * @param optimizeType Which streets to prefer.
 * @param optimizeTriangle How much time, slope and safety matter, if {@code optimizeType} is
 *                         {@link VehicleRoutingOptimizeType#TRIANGLE}.
 */
public record RidingPreferences(
  double speed,
  double reluctance,
  VehicleRoutingOptimizeType optimizeType,
  TimeSlopeSafetyTriangle optimizeTriangle
) {
  public RidingPreferences {
    Objects.requireNonNull(optimizeType);
    Objects.requireNonNull(optimizeTriangle);
  }
}
