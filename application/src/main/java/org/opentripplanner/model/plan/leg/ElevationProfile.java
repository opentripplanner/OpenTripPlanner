package org.opentripplanner.model.plan.leg;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Represents an elevation profile as a list of {@code x,y} coordinates. The {@code x} is the
 * horizontal position/distance from the first position. The {@code y} is the vertical position
 * at {@code x}.
 * <p>
 * The unit is meters and the resolution is centimeters. The profile is normalized to centi-meters.
 * <p>
 * This class is immutable, but NOT THREAD-SAFE. This is because the gained/lost properties are
 * lazy initialized. The risk of using this in a multithreaded setting in minimal, since setting
 * the gained/lost fields should be atomic and calculating it twice should result in the same
 * value.
 */
public class ElevationProfile {

  private static final double PRECISION = 100.0;

  private static final ElevationProfile EMPTY = new ElevationProfile();
  private final List<Step> steps;

  private Double gained = null;
  private Double lost = null;

  private ElevationProfile() {
    this.steps = List.of();
  }

  private ElevationProfile(Builder builder) {
    this.steps = List.copyOf(builder.steps);
  }

  public static ElevationProfile empty() {
    return EMPTY;
  }

  public static ElevationProfile.Builder of() {
    return empty().copyOf();
  }

  public ElevationProfile.Builder copyOf() {
    return new Builder(this);
  }

  /**
   * How much elevation is gained, in total, over the course of the leg, in meters. See
   * elevationLost.
   */
  public double elevationGained() {
    if (gained == null) {
      this.gained = calculateElevationChange(steps, v -> v > 0.0);
    }
    return gained;
  }

  /**
   * How much elevation is lost, in total, over the course of the leg, in meters. As an example, a
   * trip that went from the top of Mount Everest straight down to sea level, then back up K2, then
   * back down again would have an elevationLost of Everest + K2.
   */
  public double elevationLost() {
    if (lost == null) {
      this.lost = calculateElevationChange(steps, v -> v < 0.0);
    }
    return lost;
  }

  public ElevationProfile add(ElevationProfile other) {
    return copyOf().add(other).build();
  }

  /**
   * Add the given offset({@code dx}) to all x values and return a new instance.
   */
  public ElevationProfile transformX(double dx) {
    return copyOf().transformX(dx).build();
  }

  public boolean isEmpty() {
    return steps.isEmpty();
  }

  /**
   * Return true if steps are none empty and all y values are UNKNOWN.
   */
  public boolean isAllYUnknown() {
    return !isEmpty() && steps.stream().allMatch(Step::isYUnknown);
  }

  public List<Step> steps() {
    return steps;
  }

  /**
   * @return The list of elevation steps but without those elements where the y value is unknown.
   */
  public List<Step> stepsWithoutUnknowns() {
    return steps.stream().filter(step -> !step.isYUnknown()).toList();
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(ElevationProfile.class).addCol("steps", steps).toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ElevationProfile that = (ElevationProfile) o;
    return Objects.equals(steps, that.steps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(steps);
  }

  private static Double calculateElevationChange(
    List<Step> profile,
    Predicate<Double> elevationFilter
  ) {
    if (profile == null) {
      return null;
    }
    double sum = 0.0;
    boolean first = true;
    double prevElevation = 0.0;

    for (var p2 : profile) {
      double elevation = p2.y();
      if (!first) {
        double change = elevation - prevElevation;
        if (elevationFilter.test(change)) {
          sum += Math.abs(change);
        }
      }
      prevElevation = elevation;
      first = false;
    }
    return DoubleUtils.roundTo2Decimals(sum);
  }

  public static class Step {

    private static final int UNKNOWN = -9_999_999;

    private final int x;
    private final int y;

    Step(int x, int y) {
      this.x = x;
      this.y = y;
    }

    Step(double x, double y) {
      this(valueToInt(x), valueToInt(y));
    }

    Step(double x) {
      this(valueToInt(x), UNKNOWN);
    }

    /**
     * The elevation horizontal distances in meters with centi-meters resolution.
     */
    public double x() {
      return valueToDouble(x);
    }

    /**
     * The elevation vertical distances in meters with centi-meters resolution.
     * <p>
     * Return NaN is y is unknown.
     */
    public double y() {
      return y == UNKNOWN ? Double.NaN : valueToDouble(y);
    }

    public boolean isYUnknown() {
      return y == UNKNOWN;
    }

    @Override
    public String toString() {
      return "[" + x() + ", " + (isYUnknown() ? "UNKNOWN" : y()) + "]";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      var that = (Step) o;
      return that.x == x && that.y == y;
    }

    @Override
    public int hashCode() {
      return Objects.hash(x, y);
    }

    Step transformX(double dx) {
      return new Step(valueToInt(x() + dx), y);
    }

    private static int valueToInt(double v) {
      return (int) ((v + 0.005) * PRECISION);
    }

    private double valueToDouble(double v) {
      return v / PRECISION;
    }
  }

  public static class Builder {

    private final ElevationProfile original;
    private final List<Step> steps = new ArrayList<>();

    public Builder(ElevationProfile original) {
      this.original = original;
      this.steps.addAll(original.steps);
    }

    public Builder step(double x, double y) {
      if (Double.isNaN(y)) {
        return stepYUnknown(x);
      }
      this.steps.add(new Step(x, y));
      return this;
    }

    public Builder stepYUnknown(double x) {
      this.steps.add(new Step(x));
      return this;
    }

    public Builder add(ElevationProfile other) {
      this.steps.addAll(other.steps);
      return this;
    }

    /**
     * Add the given offset({@code dx}) to all x values in current set of steps in the builder.
     * Any steps added later is not transformed.
     */
    public Builder transformX(double dx) {
      steps.replaceAll(s -> s.transformX(dx));
      return this;
    }

    public ElevationProfile build() {
      if (steps.size() == original.steps.size()) {
        return original;
      }
      removeDuplicateSteps();
      return new ElevationProfile(this);
    }

    /**
     * Remove repeated values, preserving the first and last value
     */
    private void removeDuplicateSteps() {
      for (int i = steps.size() - 3; i >= 0; --i) {
        var first = steps.get(i);
        var second = steps.get(i + 1);
        var third = steps.get(i + 2);

        if (first.y == second.y && second.y == third.y) {
          steps.remove(i + 1);
        } else if (first.equals(second)) {
          steps.remove(i + 1);
        } else if (second.equals(third)) {
          steps.remove(i + 1);
        }
      }
    }
  }
}
