package org.opentripplanner.routing.api.request.preference;

import java.util.Locale;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.lang.IntUtils;

/**
 * Relax a value by the given ratio and slack. The relaxed value
 * can be calculated using this function:
 * <pre>
 *   f(x) = ratio * x + slack
 * </pre>
 *
 * @param ratio the factor to multiply into the value, must be minimum 1.0  and max 4.0
 * @param slack the amount of slack to add to the value.
 */
public record Relax(double ratio, int slack) {
  /**
   * The "normal" will produce the same result: {@code f(x) == x }
   */
  public static final Relax NORMAL = new Relax(1.0, 0);

  public Relax {
    ratio = DoubleUtils.roundTo2Decimals(ratio);
    DoubleUtils.requireInRange(ratio, 1.0, 4.0, "ratio");
    IntUtils.requireNotNegative(slack, "slack");
  }

  /**
   * {@code true} if {@link #NORMAL}, this is the same as <em>not</em> applying the function.
   * <p>
   * The relax operation should be skipped to save resources in this case, but it is
   * safe to do it.
   */
  public boolean hasNoEffect() {
    return NORMAL.equals(this);
  }

  /**
   * Opposite of {@link #hasNoEffect()}
   */
  public boolean hasEffect() {
    return !hasNoEffect();
  }

  @Override
  public String toString() {
    return String.format(Locale.ROOT, "%d + %.2f * x", slack, ratio);
  }
}
