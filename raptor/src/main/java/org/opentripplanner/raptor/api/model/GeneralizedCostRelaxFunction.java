package org.opentripplanner.raptor.api.model;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import org.opentripplanner.utils.lang.DoubleRange;
import org.opentripplanner.utils.lang.IntRange;
import org.opentripplanner.utils.lang.IntUtils;
import org.opentripplanner.utils.lang.OtpNumberFormat;
import org.opentripplanner.utils.time.DurationUtils;

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

  private static final int SLACK_PRECISSION = 100;

  /**
   * Min slack is 0s, max slack is set to the cost equivalent of riding transit for 4 hours.
   * Raptor cost is in centi-seconds.
   */
  static final IntRange SLACK_RANGE = IntRange.ofInclusive(0, FOUR_HOURS * SLACK_PRECISSION);

  /** Keep the RATIO_RESOLUTION a power of 2 for performance reasons. */
  private static final int RATIO_RESOLUTION = 0x100;
  private static final DoubleRange RATIO_RANGE = DoubleRange.of(1.0, 4.0);
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
    return of(ratio, SLACK_RANGE.startInclusive());
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
      OtpNumberFormat.formatTwoDecimals(slack / (double) SLACK_PRECISSION)
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
    if (SLACK_RANGE.isOutside(slack)) {
      throw new IllegalArgumentException(
        "Cost slack is not in range: %d not in %s".formatted(
            slack,
            SLACK_RANGE.toString(v -> DurationUtils.durationToStr(v / SLACK_PRECISSION))
          )
      );
    }
  }

  private static void assertRatioInRange(double ratio) {
    if (RATIO_RANGE.isOutside(ratio)) {
      throw new IllegalArgumentException(
        String.format(Locale.ROOT, "Cost ratio is not in range: %.2f not in %s", ratio, RATIO_RANGE)
      );
    }
  }
}
