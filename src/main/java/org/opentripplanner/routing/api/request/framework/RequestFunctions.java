package org.opentripplanner.routing.api.request.framework;

import java.io.Serializable;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.routing.api.request.RouteRequest;

/**
 * This is a factory for creating functions which can be used as parameters in the {@link
 * RouteRequest}. A function is used in the request to specify limits/thresholds. We support
 * linear function for now, but it should be easy to extend with other type of functions if needed.
 * Use the {@link #parse(String)} method to create new functions from a string.
 * <p>
 */
public class RequestFunctions {

  private static final String SEP = "\\s*";
  private static final String INT = "([\\d]+)";
  private static final String NUM = "([\\d.,]+)";
  public static final String PLUS = Pattern.quote("+");
  private static final Pattern LINEAR_FUNCTION_PATTERN = Pattern.compile(
    INT + SEP + PLUS + SEP + NUM + SEP + "[Xx]"
  );

  /** This is private to prevent this utility class from instantiation. */
  private RequestFunctions() {
    /* empty */
  }

  /**
   * Parse an input string representing a linear function on the format:
   * <p>
   * {@code a + b x}
   * <p>
   * where {@code a} is the constant and {@code b} is the coefficient.
   *
   * @throws RuntimeException if the input is not parsable.
   */
  public static CostLinearFunction parse(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }

    // Try to match to a linear function
    Matcher m = LINEAR_FUNCTION_PATTERN.matcher(text);

    if (m.find()) {
      return createLinearFunction(Integer.parseInt(m.group(1)), Double.parseDouble(m.group(2)));
    }

    // No function matched
    throw new IllegalArgumentException("Unable to parse function: '" + text + "'");
  }

  /**
   * Create a linear function of the form: {@code y = f(x) = a + b * x}. It allows setting both a
   * constant 'a' and a coefficient 'b' and the use those in the computation of a limit. The input
   * value 'x' is normally the min/max value across the sample set.
   */
  public static CostLinearFunction createLinearFunction(int constant, double coefficient) {
    return new LinearFunction(constant, coefficient);
  }

  public static String serialize(CostLinearFunction function) {
    if (function == null) {
      return null;
    }
    if (function instanceof LinearFunction) {
      return ((LinearFunction) function).serialize();
    }
    throw new IllegalArgumentException("Function type is not valid: " + function.getClass());
  }

  private static class LinearFunction implements CostLinearFunction, Serializable {

    // This class is package local to be unit testable.

    /** The constant part of the function. */
    private final int a;

    /** The coefficient part of the function. */
    private final double b;

    public LinearFunction(int constant, double coefficient) {
      this.a = constant;
      this.b = DoubleUtils.roundTo2Decimals(coefficient);
    }

    @Override
    public int calculate(int x) {
      return a + IntUtils.round(b * x);
    }

    @Override
    public String toString() {
      return serialize();
    }

    @Override
    public String serialize() {
      return LinearFunctionSerialization.serialize(Duration.ofSeconds(IntUtils.round(a)), b);
    }
  }
}
