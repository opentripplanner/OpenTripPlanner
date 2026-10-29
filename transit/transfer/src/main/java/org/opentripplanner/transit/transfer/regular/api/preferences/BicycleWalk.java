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
    return speed.normalizedDistance(other.speed) + reluctance.normalizedDistance(other.reluctance);
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
