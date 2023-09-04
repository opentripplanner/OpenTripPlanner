package org.opentripplanner.routing.api.request.framework;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.framework.lang.StringUtils;
import org.opentripplanner.framework.model.Units;
import org.opentripplanner.framework.time.DurationUtils;

/**
 * This class can serialize and parse a linear function of time/duration/cost on the form
 * {@code 3h + 1.15 t} to/from a string/text. The {@code t} is the variable in the function
 * {@code f(t)}.
 * <p>
 * This class will parse/serialize the body of a linear function definition like:
 * <pre>
 * f(x) = 2h30m15s + 1.45 x
 *       |---   body    ---|
 *</pre>
 * <p>
 * The variable name can be one of `t`, `T`, `x` or `X`.
 * <p>
 * The constant is parsed with {@link DurationUtils#duration(String)}. In the case where
 * the function is a function of cost the duration should be converted to a cost using
 * {@link org.opentripplanner.framework.model.Cost#costOfSeconds(int)}.
 */
public class LinearFunctionSerialization {

  private static final String SEP = "\\s*";
  private static final String NUM = "([\\d.,]+)";
  private static final String DUR = "(?:PT)?([\\d\\.hms]+)";
  private static final String PLUS = Pattern.quote("+");
  private static final String VARIABLE = "[XxTt]";
  private static final Pattern PATTERN = Pattern.compile(
    String.join(SEP, DUR, PLUS, NUM, VARIABLE)
  );

  private LinearFunctionSerialization() {}

  /**
   * Parse a string on the format: {@code 2m30s + 1.2 t }.
   * <p>
   * The coefficient must be a number between 0.0 and 100.0. and is normalized: if < 2.0 to
   * 2 decimals, if 2.0 < 10.0 to 1 decimal and to whole numbers above 10.0.
   * @throws NumberFormatException
   * @throws IllegalArgumentException
   */
  public static <T> Optional<T> parse(String text, BiFunction<Duration, Double, T> factory) {
    if (StringUtils.hasNoValue(text)) {
      return Optional.empty();
    }
    Matcher m = PATTERN.matcher(text);

    if (m.matches()) {
      var constantText = m.group(1);
      var coefficientText = m.group(2);
      var coefficient = Double.parseDouble(coefficientText);
      coefficient = Units.normalizedFactor(coefficient, 0.0, 100.0);

      // Unfortunately, to be backwards compatible we need to support decimal numbers.
      // If a decimal number, then the value is converted to seconds
      var constant = constantText.matches("\\d+(\\.\\d+)?")
        ? Duration.ofSeconds(IntUtils.round(Double.parseDouble(constantText)))
        : DurationUtils.duration(constantText);

      return Optional.of(factory.apply(constant, coefficient));
    }
    // No function matched
    throw new IllegalArgumentException("Unable to parse function: '" + text + "'");
  }

  public static String serialize(AbstractLinearFunction<?> value) {
    return serialize(value.constantAsDuration(), value.coefficient());
  }

  public static String serialize(Duration constant, double coefficient) {
    return String.format(
      Locale.ROOT,
      "%s + %s t",
      DurationUtils.durationToStr(constant),
      Units.factorToString(coefficient)
    );
  }
}
