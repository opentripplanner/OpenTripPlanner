package org.opentripplanner.transit.transfer.regular.api.parameters;

import org.opentripplanner.core.model.basic.NormalizedDistance;
import org.opentripplanner.transit.transfer.regular.api.maxduration.MaxDurationsPreference;

/**
 * The type of a transfer profile. The configuration that is shared by all profiles of a type, like
 * {@link MaxDurationsPreference}, is set per type. Each type has its own user preferences class,
 * for example {@link WalkPreferences} for {@link #WALK}.
 * <p>
 * The types have a distance to each other, the distance is the absolute difference of the numbers
 * of the types. It is used to find the closest profile of another type, if a request has no profile
 * of its own type. The wheelchair is the closest to the walk, then the bicycle, and the car is far
 * from all. The numbers are much bigger than the distance between two user preferences of the same
 * type, so the type decides.
 */
public enum TransferProfileType implements NormalizedDistance<TransferProfileType> {
  WALK(1000),
  WHEELCHAIR(0),
  BICYCLE(3000),
  CAR(7000);

  private final double distance;

  TransferProfileType(double distance) {
    this.distance = distance;
  }

  @Override
  public double normalizedDistance(TransferProfileType other) {
    return Math.abs(distance - other.distance);
  }
}
