package org.opentripplanner.raptor.api.model;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.framework.lang.OtpNumberFormat;

/**
 * This relax-function is used to relax increasing values by:
 * <pre>
 *   v' := v * ratio + slack
 * </pre>
 * The {@code ratio} is rounded of to the closest 1/16. This is done for
 * performance reasons since we then can use shift-right 4 bit to divide by 16.
 * <p>
 * "Increasing" means that {@code v} is better than {@code u}, if {@code v < u}.
 */
public final class GeneralizedCostRelaxFunction implements RelaxFunction {

  private static final int FOUR_HOURS = (int) Duration.ofHours(4).toSeconds();

  public static final int MIN_SLACK = 0;
  /**
   * Max cost slack is set to the cost equivalent of riding transit for 1 hour.
   * Raptor cost is in centi-seconds.
   */
  public static final int MAX_SLACK = FOUR_HOURS * 100;

  /** Keep the RATIO_RESOLUTION a power of 2 for performance reasons. */
  private static final int RATIO_RESOLUTION = 0x100;
  public static final double MIN_RATIO = 1.0;
  public static final double MAX_RATIO = 4.0;
  private final int normalizedRatio;
  private final int slack;

  private GeneralizedCostRelaxFunction(int normalizedRatio, int slack) {
    this.normalizedRatio = normalizedRatio;
    this.slack = slack;
  }

  /**
   * Create a relax function for increasing cost values. The relax function will add to the
   * value passed into it.
   */
  public static RelaxFunction of(double ratio, int slack) {
    assertRatioInRange(ratio);
    assertSlackInRange(slack);

    int normalizedRatio = normalizedRatio(ratio);

    if (isNormal(normalizedRatio, slack)) {
      return NORMAL;
    }
    return new GeneralizedCostRelaxFunction(normalizedRatio, slack);
  }

  public static RelaxFunction of(double ratio) {
    return of(ratio, MIN_SLACK);
  }

  public int relax(int value) {
    return ((value * normalizedRatio) / RATIO_RESOLUTION) + slack;
  }

  @Override
  public String toString() {
    return String.format(
      Locale.ROOT,
      "f(x) = %.2f * x + %s",
      normalizedRatio / (double) RATIO_RESOLUTION,
      OtpNumberFormat.formatTwoDecimals(slack / 100.0)
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GeneralizedCostRelaxFunction that = (GeneralizedCostRelaxFunction) o;
    return normalizedRatio == that.normalizedRatio && slack == that.slack;
  }

  @Override
  public int hashCode() {
    return Objects.hash(normalizedRatio, slack);
  }

  private static int normalizedRatio(double ratio) {
    return IntUtils.round(ratio * RATIO_RESOLUTION);
  }

  private static boolean isNormal(int normalizedRatio, int slack) {
    return normalizedRatio == RATIO_RESOLUTION && slack == 0;
  }

  private static void assertSlackInRange(int slack) {
    if (slack < MIN_SLACK || slack > MAX_SLACK) {
      throw new IllegalArgumentException(
        "Cost slack is not in range: %d not in [%d..%d]".formatted(slack, MIN_SLACK, MAX_SLACK)
      );
    }
  }

  private static void assertRatioInRange(double ratio) {
    if (ratio < MIN_RATIO || ratio > MAX_RATIO) {
      throw new IllegalArgumentException(
        String.format(
          Locale.ROOT,
          "Cost ratio is not in range: %.2f not in [%.1f..%.1f]",
          ratio,
          MIN_RATIO,
          MAX_RATIO
        )
      );
    }
  }
}
