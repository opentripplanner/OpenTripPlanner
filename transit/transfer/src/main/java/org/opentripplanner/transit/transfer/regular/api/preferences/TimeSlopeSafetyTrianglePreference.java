package org.opentripplanner.transit.transfer.regular.api.preferences;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.core.model.basic.NormalizedDistance;
import org.opentripplanner.core.model.basic.NormalizedRatio;
import org.opentripplanner.core.model.basic.Ratio;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The relative importance of the time, the slope and the safety when a vehicle (a bicycle, later a
 * scooter) is routed. One of the three is the same as the optimization for the shortest duration
 * (time), the flattest streets (slope) or the safest streets (safety).
 * <p>
 * The values are normalized: they sum to exactly 1.0, and each is a multiple of 5/100, so the class
 * can be used as a part of a cache key. The values given to {@link #of(double, double, double)} are
 * relative to each other, for example {@code of(0.1, 0, 0)} is the same as {@code of(1, 0, 0)}.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class TimeSlopeSafetyTrianglePreference
  implements NormalizedDistance<TimeSlopeSafetyTrianglePreference>, Serializable
{

  /** The number of steps the sum of the values (1.0) is divided into, each step is 5/100. */
  private static final int STEPS = 20;

  private static final double STEP = 1.0 / STEPS;

  private final NormalizedRatio time;
  private final NormalizedRatio slope;
  private final NormalizedRatio safety;

  private TimeSlopeSafetyTrianglePreference(int time, int slope, int safety) {
    this.time = ratio(time);
    this.slope = ratio(slope);
    this.safety = ratio(safety);
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
    // Share out the steps: each value gets the whole steps of its share, and the steps that are
    // left go to the values with the biggest remainder.
    double[] share = { (time / sum) * STEPS, (slope / sum) * STEPS, (safety / sum) * STEPS };
    int[] steps = new int[3];
    int used = 0;
    for (int i = 0; i < 3; ++i) {
      steps[i] = (int) Math.floor(share[i] + 1e-9);
      used += steps[i];
    }
    for (int left = STEPS - used; left > 0; --left) {
      int best = 0;
      double bestRemainder = -1.0;
      for (int i = 0; i < 3; ++i) {
        double remainder = share[i] - steps[i];
        if (remainder > bestRemainder + 1e-9) {
          best = i;
          bestRemainder = remainder;
        }
      }
      steps[best]++;
    }
    return new TimeSlopeSafetyTrianglePreference(steps[0], steps[1], steps[2]);
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

  @Override
  public float normalizedDistance(TimeSlopeSafetyTrianglePreference other) {
    return (
      time.normalizedDistance(other.time) +
      slope.normalizedDistance(other.slope) +
      safety.normalizedDistance(other.safety)
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

  private static NormalizedRatio ratio(int steps) {
    return Ratio.of(steps * STEP).normalize();
  }
}
