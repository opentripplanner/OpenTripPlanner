package org.opentripplanner.standalone.config.routerconfig.updaters;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_11;

import java.time.Duration;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.trip.siri.SiriFuzzyTripMatchingParameters;

/**
 * Parse the fuzzy trip matching configuration shared by all SIRI-ET updaters.
 * <p>
 * The deprecated boolean {@code fuzzyTripMatching} is parsed by each updater, because its
 * version and default differ between the updaters. This class parses the {@code fuzzyMatching}
 * object, which replaces it. A separate parameter, instead of an object form of
 * {@code fuzzyTripMatching}, lets the same configuration file be used with older OTP versions:
 * they ignore {@code fuzzyMatching} and use {@code fuzzyTripMatching}.
 */
public class SiriFuzzyTripMatchingConfig {

  private static final String PARAM_NAME = "fuzzyMatching";
  private static final String EMPTY_OBJECT = "{}";

  /** The summary of the deprecated {@code fuzzyTripMatching} parameter. */
  public static final String FUZZY_TRIP_MATCHING_SUMMARY =
    "**Deprecated**, use `fuzzyMatching`. If the fuzzy trip matcher should be used to match trips.";

  /** The description of the deprecated {@code fuzzyTripMatching} parameter. */
  public static final String FUZZY_TRIP_MATCHING_DESCRIPTION = """
  `true` is the same as `"fuzzyMatching" : {}`. This parameter is ignored if `fuzzyMatching` is
  set, so both can be set in a configuration used by OTP versions with and without
  `fuzzyMatching`.
  """;

  private static final String FUZZY_MATCHING_DESCRIPTION = """
  The fuzzy trip matcher identifies the scheduled trip of a SIRI-ET message by its last stop,
  line, service date and the aimed times of the first and last call. Use it for producers whose
  messages do not contain usable trip ids.

  If this parameter is set, fuzzy trip matching is enabled with the given options and the
  deprecated `fuzzyTripMatching` parameter is ignored. Use an empty object `{}` to enable fuzzy
  trip matching with the default options.

  If a trip matches the aimed times exactly, it is used. Otherwise, if more than one trip matches
  within the time tolerance, the message is rejected.
  """;

  private static final String TIME_TOLERANCE_DESCRIPTION = """
  A scheduled trip matches if its scheduled departure time at the first stop and its scheduled
  arrival time at the last stop differ from the SIRI aimed times by at most this duration. Use
  this if the producer sends aimed times in whole minutes, while the schedule contains seconds.
  The tolerance must be in whole seconds and less than 30 minutes. A larger tolerance increases
  the chance that more than one trip matches, in which case the message is rejected.
  """;

  /**
   * Parse the {@code fuzzyMatching} parameter.
   *
   * @param fuzzyTripMatching The value of the deprecated {@code fuzzyTripMatching} parameter. It
   *                          is used if {@code fuzzyMatching} is not set.
   */
  public static SiriFuzzyTripMatchingParameters map(NodeAdapter c, boolean fuzzyTripMatching) {
    var node = c
      .of(PARAM_NAME)
      .since(V2_11)
      .summary("Enable the fuzzy trip matcher, with options.")
      .description(FUZZY_MATCHING_DESCRIPTION)
      .asObject();

    Duration timeTolerance = node
      .of("timeTolerance")
      .since(V2_11)
      .summary("The maximum difference between the scheduled and the aimed times.")
      .description(TIME_TOLERANCE_DESCRIPTION)
      .asDuration(Duration.ZERO);

    if (node.isEmpty()) {
      return SiriFuzzyTripMatchingParameters.of(fuzzyTripMatching);
    }
    // asObject() accepts any JSON type, so a value like `false` would enable fuzzy matching with
    // the default options. NodeAdapter.isObject() is false for the empty object, which is valid.
    if (!node.isObject() && !EMPTY_OBJECT.equals(node.toJson())) {
      throw c.createException(
        "The parameter value '%s' must be an object, for example {}.".formatted(node.toJson()),
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
