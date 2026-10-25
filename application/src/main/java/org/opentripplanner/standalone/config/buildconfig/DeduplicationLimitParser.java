package org.opentripplanner.standalone.config.buildconfig;

import java.util.regex.Pattern;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.transit.transfer.regular.api.parameters.DeduplicationLimit;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * Parses the {@code deduplicationLimit} of the transfer configuration, a linear function of a cost
 * like {@code 0.005x + 30s}. The terms may be in any order, and one of them may be left out, for
 * example {@code 30s} or {@code 0.01x}. The variable is {@code x} or {@code t}.
 * <p>
 * The coefficient keeps its exact value. The linear functions of the routing request are not used,
 * because they round the coefficient to a step of 0.1, which is far too coarse for a share of a
 * cost.
 */
final class DeduplicationLimitParser {

  private static final Pattern COEFFICIENT_TERM = Pattern.compile("([\\d.]+)\\s*[XxTt]");

  private DeduplicationLimitParser() {}

  /** @throws IllegalArgumentException if the text can not be parsed */
  static DeduplicationLimit parse(String text) {
    Cost constant = null;
    Double coefficient = null;
    for (var term : text.split("\\+", -1)) {
      var trimmed = term.trim();
      var coefficientTerm = COEFFICIENT_TERM.matcher(trimmed);
      if (coefficientTerm.matches()) {
        coefficient = single(coefficient, parseCoefficient(coefficientTerm.group(1), text), text);
      } else {
        constant = single(constant, parseConstant(trimmed, text), text);
      }
    }
    return new DeduplicationLimit(
      constant == null ? Cost.ZERO : constant,
      coefficient == null ? 0.0 : coefficient
    );
  }

  private static <T> T single(T existing, T value, String text) {
    if (existing != null) {
      throw new IllegalArgumentException(
        "Unable to parse the deduplication limit '" +
          text +
          "': it can have one constant and one coefficient."
      );
    }
    return value;
  }

  private static double parseCoefficient(String number, String text) {
    try {
      return Double.parseDouble(number);
    } catch (NumberFormatException e) {
      throw invalid(text);
    }
  }

  private static Cost parseConstant(String term, String text) {
    try {
      return Cost.fromDuration(DurationUtils.duration(term));
    } catch (RuntimeException e) {
      throw invalid(text);
    }
  }

  private static IllegalArgumentException invalid(String text) {
    return new IllegalArgumentException(
      "Unable to parse the deduplication limit '" + text + "', use for example '0.005x + 30s'."
    );
  }
}
