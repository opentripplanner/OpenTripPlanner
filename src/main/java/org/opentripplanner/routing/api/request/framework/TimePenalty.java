package org.opentripplanner.routing.api.request.framework;

import java.time.Duration;
import java.util.Objects;
import org.opentripplanner.framework.lang.StringUtils;
import org.opentripplanner.framework.model.Units;
import org.opentripplanner.framework.time.DurationUtils;

/** See {@link #DOC}. */
public class TimePenalty {

  public static final String DOC =
    """
    The time penalty is a linear function applied to the actual-time/duration of the leg. The time
    penalty consist of a `constant` and a `coefficient`. The penalty is not added to the actual
    time, like a slack. Instead, the penalty is *invisible* in the returned itinerary, but it is
    applied during routing.
        
    The penalty is a function of time(duration):
    ```
    f(t) = a + b * t
    ```
    where `a` is the constant time part, `b` is the time-coefficient. `f(t)` is the function to
    calculate the penalty-time. The penalty-time is added to the actual-time during routing. If
    `a=0s` and `b=0.0`, then the penalty is `0`(zero).
    
    Examples: `0s + 2.5t`, `10m + 0t` and `1h5m59s + 9.9t`
    
    The `constant` must be 0 or a positive duration.
    The `coefficient` must be in range `[0.0, 10.0]`.
    """;

  public static final TimePenalty ZERO = new TimePenalty(Duration.ZERO, 0.0);

  private final Duration constant;
  private final double coefficient;

  private TimePenalty(Duration constant, double coefficient) {
    this.constant = DurationUtils.requireNonNegative(constant);
    this.coefficient = Units.normalizedFactor(coefficient, 0.0, 10.0);
  }

  public static TimePenalty of(Duration constant, double coefficient) {
    return new TimePenalty(constant, coefficient);
  }

  /**
   * Parse a string on the format: {@code 2m30s + 1.2 t ; 1.0 c }.
   */
  public static TimePenalty of(String text) {
    if (StringUtils.hasNoValue(text)) {
      return ZERO;
    }
    return LinearFunctionOfTimeParser.parse(text, TimePenalty::new).orElse(ZERO);
  }

  public boolean isZero() {
    return Duration.ZERO.equals(constant) && coefficient == 0.0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TimePenalty that = (TimePenalty) o;
    return Objects.equals(that.coefficient, coefficient) && Objects.equals(constant, that.constant);
  }

  @Override
  public int hashCode() {
    return Objects.hash(constant, coefficient);
  }

  @Override
  public String toString() {
    return LinearFunctionOfTimeParser.serialize(constant, coefficient);
  }

  public Duration calculate(int timeInSeconds) {
    return Duration.ofSeconds(constant.toSeconds() + Math.round(coefficient * timeInSeconds));
  }
}
