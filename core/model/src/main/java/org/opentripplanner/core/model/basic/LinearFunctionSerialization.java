package org.opentripplanner.core.model.basic;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.lang.IntUtils;
import org.opentripplanner.utils.lang.StringUtils;
import org.opentripplanner.utils.time.DurationUtils;

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
 * The constant is parsed with {@link DurationUtils#duration(String)}. In the case where the
 * function is a function of cost the duration should be converted to a cost using
 * {@link Cost#costOfSeconds(int)}.
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

  private static final Pattern DECIMAL_NUMBER_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");

  private LinearFunctionSerialization() {}

  /**
   * Parse a string on the format: {@code 2m30s + 1.2 t }.
   * <p>
   * The coefficient must be a number in the range of {@link AbstractLinearFunction}, and is rounded
   * to the step of its bucket in the coefficient scale: finer steps for small values, coarser for
   * large.
   *
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
      coefficient = AbstractLinearFunction.normalizeCoefficient(coefficient);

      var constant = parseDecimalSecondsOrDuration(constantText);

      return Optional.of(factory.apply(constant, coefficient));
    }
    // No function matched
    throw new IllegalArgumentException("Unable to parse function: '" + text + "'");
  }

  /**
   * Serialize the function. The coefficient is written with the number of decimals of the step of
   * its bucket in the given {@code scale}, see {@link DoubleUtils#roundToStep(double, double[][])}
   * for the table format. A coefficient is written with at least one decimal, if the step is less
   * than 1, so the serialized text can be parsed back to the same function.
   */
  public static String serialize(AbstractLinearFunction<?> value, double[][] scale) {
    return serialize(value.constantAsDuration(), value.coefficient(), scale);
  }

  /** Serialize with the coefficient scale of the linear functions. */
  public static String serialize(Duration constant, double coefficient) {
    return serialize(constant, coefficient, AbstractLinearFunction.COEFFICIENT_SCALE);
  }

  static String serialize(Duration constant, double coefficient, double[][] scale) {
    return String.format(
      Locale.ROOT,
      "%s + %s t",
      DurationUtils.durationToStr(constant),
      coefficientToString(coefficient, scale)
    );
  }

  private static String coefficientToString(double coefficient, double[][] scale) {
    int decimals = 0;
    for (var row : scale) {
      if (coefficient < row[0]) {
        decimals = Math.max(0, BigDecimal.valueOf(row[1]).stripTrailingZeros().scale());
        break;
      }
    }
    var text = String.format(Locale.ROOT, "%." + decimals + "f", coefficient);
    if (decimals > 1) {
      int end = text.length();
      while (text.charAt(end - 1) == '0' && text.charAt(end - 2) != '.') {
        --end;
      }
      text = text.substring(0, end);
    }
    return text;
  }

  /**
   * Parse a String as a Duration. Unfortunately, to be backwards compatible we need to support
   * decimal numbers. If the text represents a decimal number, then the value is converted to
   * seconds. <br/>
   * The parsing function {@link DurationUtils#parseSecondsOrDuration(String)} cannot be used here
   * since it supports only integer seconds, not decimal seconds.
   *
   */
  private static Duration parseDecimalSecondsOrDuration(String text) {
    try {
      if (DECIMAL_NUMBER_PATTERN.matcher(text).matches()) {
        return Duration.ofSeconds(IntUtils.round(Double.parseDouble(text)));
      }
      return DurationUtils.duration(text);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Unable to parse duration: '" + text + "'");
    }
  }
}
