package org.opentripplanner.routing.api.request.framework;

import java.time.Duration;
import org.opentripplanner.framework.lang.StringUtils;
import org.opentripplanner.framework.time.DurationUtils;

public final class TimePenalty extends AbstractLinearFunction<Duration> {

  public static final TimePenalty ZERO = new TimePenalty(Duration.ZERO, 0.0);

  private TimePenalty(Duration constant, double coefficient) {
    super(DurationUtils.requireNonNegative(constant), coefficient);
  }

  public static TimePenalty of(Duration constant, double coefficient) {
    return new TimePenalty(constant, coefficient);
  }

  /**
   * @see LinearFunctionSerialization
   */
  public static TimePenalty of(String text) {
    if (StringUtils.hasNoValue(text)) {
      return ZERO;
    }
    return LinearFunctionSerialization.parse(text, TimePenalty::new).orElse(ZERO);
  }

  @Override
  protected boolean isZero(Duration value) {
    return value.isZero();
  }

  @Override
  protected Duration constantAsDuration() {
    return constant();
  }

  public Duration calculate(Duration time) {
    return constant().plusSeconds(Math.round(coefficient() * time.toSeconds()));
  }
}
