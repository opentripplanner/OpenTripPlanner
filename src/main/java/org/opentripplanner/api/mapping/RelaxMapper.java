package org.opentripplanner.api.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.opentripplanner.routing.api.request.preference.Relax;

/**
 * Map a text to a Relax instance. The patter used is:
 * <pre>
 *   NNN.NN '*' [variable-placeholder] '+' NNN
 * </pre>
 * {@code NNN.NN} is any positive decimal number.
 * {@code NNN} is any positive integer number.
 * {@code variable-placeholder} is any alphabetical variable name - this is just a placeholder
 *                              to make it clear what is the ratio/factor and what is the
 *                              constant/slack.
 */
public class RelaxMapper {

  private static final String SEP = "\\s*";
  private static final String NUM = "(\\d+(?:\\.\\d+)?+)";
  private static final String INT = "(\\d+)";
  private static final String ALFA = "[a-zA-Z]+";
  private static final String PLUS = SEP + Pattern.quote("+") + SEP;
  private static final String TIMES = Pattern.quote("*");

  private static final String RATIO = NUM + SEP + TIMES + "?" + SEP + ALFA;
  private static final String SLACK = INT;

  private static final Pattern RELAX_PATTERN_1 = Pattern.compile(RATIO + PLUS + SLACK);
  private static final Pattern RELAX_PATTERN_2 = Pattern.compile(SLACK + PLUS + RATIO);

  public static Relax mapRelax(String input) {
    if (input == null || input.isBlank()) {
      return null;
    }
    String inputTrimmed = input.trim();
    List<Exception> errors = new ArrayList<>();

    return parse(RELAX_PATTERN_1, inputTrimmed, 1, 2, errors)
      .or(() -> parse(RELAX_PATTERN_2, inputTrimmed, 2, 1, errors))
      .orElseThrow(() ->
        new IllegalArgumentException(
          "Unable to parse function: '" +
          input +
          "'. Use: '2.0 * x + 100'." +
          (errors.isEmpty() ? "" : " Details: " + errors)
        )
      );
  }

  public static String mapRelaxToString(Relax domain) {
    return String.format(Locale.ROOT, "%.2f * x + %d", domain.ratio(), domain.slack());
  }

  private static Optional<Relax> parse(
    Pattern pattern,
    String input,
    int ratioIdx,
    int slackIdx,
    List<Exception> errors
  ) {
    var m = pattern.matcher(input);
    if (m.matches()) {
      try {
        return Optional.of(
          new Relax(Double.parseDouble(m.group(ratioIdx)), Integer.parseInt(m.group(slackIdx)))
        );
      } catch (Exception e) {
        errors.add(e);
      }
    }
    return Optional.empty();
  }
}
