package org.opentripplanner.transit.transfer.regular.api.preferences;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.core.model.basic.NormalizedDistance;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The preferences for walking with a bicycle, when the traveler pushes it, for example on stairs.
 * The values are normalized(rounded), so the class can be used as a part of a cache key.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class BicycleWalk implements NormalizedDistance<BicycleWalk>, Serializable {

  private final Speed speed;
  private final Reluctance reluctance;

  /**
   * Walking with the bicycle counts a third of what the speed and the reluctance of the bicycle
   * count. The speed and the reluctance have the reference distance for a change of 1.3 to 1.4 m/s
   * and 2.0 to 2.5, like the walking.
   */
  private static final double SPEED_WEIGHT =
    DistanceWeight.of(Speed.ofMetersPerSecond(1.3), Speed.ofMetersPerSecond(1.4)) / 3.0;

  private static final double RELUCTANCE_WEIGHT =
    DistanceWeight.of(Reluctance.of(2.0), Reluctance.of(2.5)) / 3.0;

  private BicycleWalk(Speed speed, Reluctance reluctance) {
    this.speed = Objects.requireNonNull(speed);
    this.reluctance = Objects.requireNonNull(reluctance);
  }

  public static BicycleWalk of(Speed speed, Reluctance reluctance) {
    return new BicycleWalk(speed, reluctance);
  }

  /** The speed of walking with the bicycle. */
  public Speed speed() {
    return speed;
  }

  /** A multiplier for the cost of walking with the bicycle. A higher value avoids walking. */
  public Reluctance reluctance() {
    return reluctance;
  }

  @Override
  public double normalizedDistance(BicycleWalk other) {
    return (
      SPEED_WEIGHT * speed.normalizedDistance(other.speed) +
      RELUCTANCE_WEIGHT * reluctance.normalizedDistance(other.reluctance)
    );
  }

  @Override
  public boolean equals(Object o) {
    return (
      this == o ||
      (o instanceof BicycleWalk that &&
        speed.equals(that.speed) &&
        reluctance.equals(that.reluctance))
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(speed, reluctance);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(BicycleWalk.class)
      .addObj("speed", speed)
      .addObj("reluctance", reluctance)
      .toString();
  }
}
