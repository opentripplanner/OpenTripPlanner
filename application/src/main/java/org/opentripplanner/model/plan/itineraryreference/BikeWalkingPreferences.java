package org.opentripplanner.model.plan.itineraryreference;

import java.time.Duration;
import java.util.Objects;
import org.opentripplanner.core.model.basic.Cost;

/**
 * How a client wants to walk a rented bike, for example where riding is not allowed.
 *
 * @param speed The walking speed in meters per second.
 * @param reluctance The cost factor for time spent walking the bike.
 * @param mountDismountTime The time it takes to get on or off the bike.
 * @param mountDismountCost The cost of getting on or off the bike.
 */
public record BikeWalkingPreferences(
  double speed,
  double reluctance,
  Duration mountDismountTime,
  Cost mountDismountCost
) {
  public BikeWalkingPreferences {
    Objects.requireNonNull(mountDismountTime);
    Objects.requireNonNull(mountDismountCost);
  }
}
