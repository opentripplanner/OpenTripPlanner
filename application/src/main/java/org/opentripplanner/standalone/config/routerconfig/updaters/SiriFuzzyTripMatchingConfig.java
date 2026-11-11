package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_11;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;
import org.opentripplanner.updater.trip.siri.SiriFuzzyTripMatchingParameters;

/**
 * Parse the {@code fuzzyTripMatching} parameter shared by all SIRI-ET updaters. The parameter
 * accepts a boolean for backwards compatibility, or an object with matching options.
 */
public class SiriFuzzyTripMatchingConfig {

  private static final String PARAM_NAME = "fuzzyTripMatching";

  private static final String DESCRIPTION = """
  The fuzzy trip matcher identifies the scheduled trip of a SIRI-ET message by its last stop,
  line, service date and the aimed times of the first and last call. Use it for producers whose
  messages do not contain usable trip ids.

  The parameter accepts a boolean or an object:

  - Missing or `false`: no fuzzy trip matching.
  - `true`: fuzzy trip matching where the scheduled times must be equal to the aimed times, to
    the second. This is the same as `{ "timeTolerance" : "0s" }`.
  - An object: fuzzy trip matching with the given options, for example
    `{ "timeTolerance" : "59s" }`.

  If more than one trip matches, the message is rejected.
  """;

  private static final String TIME_TOLERANCE_DESCRIPTION = """
  A scheduled trip matches if its scheduled departure time at the first stop and its scheduled
  arrival time at the last stop differ from the SIRI aimed times by at most this duration. Use
  this if the producer sends aimed times in whole minutes, while the schedule contains seconds.
  The tolerance must be in whole seconds and less than 30 minutes. A larger tolerance increases
  the chance that more than one trip matches, in which case the message is rejected.
  """;

  /**
   * @param since The OTP version where the {@code fuzzyTripMatching} parameter was added to the
   *              updater.
   */
  public static SiriFuzzyTripMatchingParameters map(NodeAdapter c, OtpVersion since) {
    var node = c
      .of(PARAM_NAME)
      .since(since)
      .summary(
        "Whether and how the fuzzy trip matcher should be used. A boolean or an object with options."
      )
      .description(DESCRIPTION)
      .asObject();

    // Register the child parameters before returning for the boolean form, so they are documented
    // even if the example config uses `true`. The generated documentation only lists them if
    // fuzzyTripMatching is present in the example router-config.json used by the doc tests.
    Duration timeTolerance = node
      .of("timeTolerance")
      .since(V2_11)
      .summary("The maximum difference between the scheduled and the aimed times.")
      .description(TIME_TOLERANCE_DESCRIPTION)
      .asDuration(Duration.ZERO);

    if (node.isEmpty()) {
      return SiriFuzzyTripMatchingParameters.DISABLED;
    }
    if (node.isBoolean()) {
      return SiriFuzzyTripMatchingParameters.of(Boolean.parseBoolean(node.asText()));
    }
    if (!node.isJsonObject()) {
      throw c.createException(
        "The parameter value '%s' must be a boolean or an object.".formatted(node.toJson()),
        PARAM_NAME
      );
    }
    try {
      return SiriFuzzyTripMatchingParameters.ofTimeTolerance(timeTolerance);
    } catch (IllegalArgumentException e) {
      throw node.createException("Invalid time tolerance.", "timeTolerance", e);
    }
  }
}
