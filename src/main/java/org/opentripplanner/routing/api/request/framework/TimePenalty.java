package org.opentripplanner.routing.api.request.framework;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

  private static final String SEP = "\\s*";
  private static final String NUM = "([\\d.,]+)";
  private static final String DUR = "(?:PT)?((?:[\\d]+[hms]?)+)";
  private static final String PLUS = Pattern.quote("+");
  private static final String TIME_VARIABLE = "[XxTt]";
  private static final Pattern PATTERN = Pattern.compile(
    String.join(SEP, DUR, PLUS, NUM, TIME_VARIABLE)
  );

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
    Matcher m = PATTERN.matcher(text);

    if (m.matches()) {
      return TimePenalty.of(DurationUtils.duration(m.group(1)), Double.parseDouble(m.group(2)));
    }
    // No function matched
    throw new IllegalArgumentException("Unable to parse time-penalty: '" + text + "'");
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
    return String.format(
      Locale.ROOT,
      "%s + %s t",
      DurationUtils.durationToStr(constant),
      Units.factorToString(coefficient)
    );
  }

  public Duration calculate(int timeInSeconds) {
    return calculateNewTimeInSeconds(timeInSeconds);
  }

  private Duration calculateNewTimeInSeconds(int timeInSeconds) {
    return Duration.ofSeconds(constant.toSeconds() + Math.round(coefficient * timeInSeconds));
  }
}
