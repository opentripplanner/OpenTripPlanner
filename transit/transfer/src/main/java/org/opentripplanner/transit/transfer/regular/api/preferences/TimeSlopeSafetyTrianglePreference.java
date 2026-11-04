package org.opentripplanner.transit.transfer.regular.api.preferences;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.core.model.basic.NormalizedDistance;
import org.opentripplanner.core.model.basic.NormalizedRatio;
import org.opentripplanner.core.model.basic.Ratio;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The relative importance of the time, the slope and the safety when a vehicle (a bicycle) is
 * routed. One of the three is the same as the optimization for the shortest duration
 * (time), the flattest streets (slope) or the safest streets (safety).
 * <p>
 * The values are normalized: they sum to exactly 1.0, and each is a multiple of {@link NormalizedRatio#STEP}, so the class
 * can be used as a part of a cache key. The values given to {@link #of(double, double, double)} are
 * relative to each other, for example {@code of(0.1, 0, 0)} is the same as {@code of(1, 0, 0)}.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class TimeSlopeSafetyTrianglePreference
  implements NormalizedDistance<TimeSlopeSafetyTrianglePreference>, Serializable
{

  /** The weight of the safety in the distance if the user wants less safety than the profile. */
  private static final double LESS_SAFETY_WEIGHT = 3.0;

  /** The weight of the safety in the distance if the user wants more safety than the profile. */
  private static final double MORE_SAFETY_WEIGHT = 8.0;

  private final NormalizedRatio time;
  private final NormalizedRatio slope;
  private final NormalizedRatio safety;

  private TimeSlopeSafetyTrianglePreference(
    NormalizedRatio time,
    NormalizedRatio slope,
    NormalizedRatio safety
  ) {
    this.time = time;
    this.slope = slope;
    this.safety = safety;
  }

  /**
   * Create a triangle from values relative to each other.
   *
   * @throws IllegalArgumentException if a value is negative, or if all values are zero
   */
  public static TimeSlopeSafetyTrianglePreference of(double time, double slope, double safety) {
    if (time < 0.0 || slope < 0.0 || safety < 0.0) {
      throw new IllegalArgumentException(
        "The time, slope and safety must not be negative: " + time + ", " + slope + ", " + safety
      );
    }
    double sum = time + slope + safety;
    if (!(sum > 0.0)) {
      throw new IllegalArgumentException("At least one of time, slope and safety must be set.");
    }
    var timeRatio = Ratio.of(time / sum).normalize();
    var slopeRatio = Ratio.of(slope / sum).normalize();
    var safetyRatio = Ratio.of(1.0 - (timeRatio.asDouble() + slopeRatio.asDouble())).normalize();

    return new TimeSlopeSafetyTrianglePreference(timeRatio, slopeRatio, safetyRatio);
  }

  /** The importance of the time, a value of 1.0 is the same as the shortest duration. */
  public NormalizedRatio time() {
    return time;
  }

  /** The importance of the slope, a value of 1.0 is the same as the flattest streets. */
  public NormalizedRatio slope() {
    return slope;
  }

  /** The importance of the safety, a value of 1.0 is the same as the safest streets. */
  public NormalizedRatio safety() {
    return safety;
  }

  /**
   * {@inheritDoc}
   * <p>
   * The distance is from this triangle, of the user, to the triangle {@code other} of a profile.
   * The time and the slope count 1, and the safety counts {@link #LESS_SAFETY_WEIGHT} if the user
   * wants less safety than the profile has, and {@link #MORE_SAFETY_WEIGHT} if the user wants
   * more. The distance is therefore <b>asymmetric</b>.
   * <p>
   * The time and the slope are a fair substitute for each other, but neither is for the safety.
   * A safe profile used for a user with a flat triangle costs 23% extra, a time profile used for a
   * flat user only 3-5%. The cost grows steeply with the difference in safety, so the safety is
   * weighted high. A path found with less safety than the user wants is much worse than one found
   * with more safety, so the weight is larger when the user wants more: a user with a triangle of
   * one third each (stored as 0.35, 0.35 and 0.30) is then closer to a safe profile than to a fast
   * one. Both weights are rough defaults, how much the safety matters depends on the safety
   * values of the street data (the OSM tag mapping).
   */
  @Override
  public double normalizedDistance(TimeSlopeSafetyTrianglePreference other) {
    double safetyWeight =
      safety.asDouble() > other.safety.asDouble() ? MORE_SAFETY_WEIGHT : LESS_SAFETY_WEIGHT;
    return (
      time.normalizedDistance(other.time) +
      slope.normalizedDistance(other.slope) +
      safetyWeight * safety.normalizedDistance(other.safety)
    );
  }

  @Override
  public boolean equals(Object o) {
    return (
      this == o ||
      (o instanceof TimeSlopeSafetyTrianglePreference that &&
        time.equals(that.time) &&
        slope.equals(that.slope) &&
        safety.equals(that.safety))
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(time, slope, safety);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TimeSlopeSafetyTrianglePreference.class)
      .addObj("time", time)
      .addObj("slope", slope)
      .addObj("safety", safety)
      .toString();
  }
}
