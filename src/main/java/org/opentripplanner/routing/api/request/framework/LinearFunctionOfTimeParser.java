package org.opentripplanner.routing.api.request.framework;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.framework.lang.StringUtils;
import org.opentripplanner.framework.model.Units;
import org.opentripplanner.framework.time.DurationUtils;

/**
 * Parse a linear function of time/duration/cost on the form: {@code 3h + 1.15 t}.
 */
public class LinearFunctionOfTimeParser {

  private static final String SEP = "\\s*";
  private static final String NUM = "([\\d.,]+)";
  private static final String DUR = "(?:PT)?((?:[\\d]+[hms]?)+)";
  private static final String PLUS = Pattern.quote("+");
  private static final String VARIABLE = "[XxTt]";
  private static final Pattern PATTERN = Pattern.compile(
    String.join(SEP, DUR, PLUS, NUM, VARIABLE)
  );

  private LinearFunctionOfTimeParser() {}

  /**
   * Parse a string on the format: {@code 2m30s + 1.2 t ; 1.0 c }.
   */
  public static <T> Optional<T> parse(String text, BiFunction<Duration, Double, T> factory) {
    if (StringUtils.hasNoValue(text)) {
      return Optional.empty();
    }
    Matcher m = PATTERN.matcher(text);

    if (m.matches()) {
      var constantText = m.group(1);
      var coefficient = Double.parseDouble(m.group(2));

      var constant = constantText.matches("\\d+")
        ? Duration.ofSeconds(Integer.parseInt(constantText))
        : DurationUtils.duration(constantText);

      return Optional.of(factory.apply(constant, coefficient));
    }
    // No function matched
    throw new IllegalArgumentException("Unable to parse function: '" + text + "'");
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
