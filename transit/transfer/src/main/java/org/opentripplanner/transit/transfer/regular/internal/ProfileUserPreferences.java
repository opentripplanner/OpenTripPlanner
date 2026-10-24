package org.opentripplanner.transit.transfer.regular.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.TransferProfilesParameters;
import org.opentripplanner.transit.transfer.regular.api.WalkProfileParameters;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;

/**
 * The user preferences of the configured profiles. A profile without preferences of its own gets
 * the preferences of its routing request.
 */
public final class ProfileUserPreferences {

  private ProfileUserPreferences() {}

  /** The user preferences of the profile: its own, or else the ones of its routing request. */
  static <R> AbstractUserPreferences<?> resolve(
    WalkProfileParameters<R> profile,
    TransferRequestMapper<R> mapper
  ) {
    var own = profile.userPreferences();
    return own != null ? own : mapper.toUserPreferences(profile.routingRequest());
  }

  /**
   * The profiles by their user preferences, in the order they are declared. The user preferences
   * identify a profile in the repository, so two profiles with equal preferences are rejected.
   */
  public static <R> Map<AbstractUserPreferences<?>, WalkProfileParameters<R>> resolveAll(
    TransferProfilesParameters<R> parameters,
    TransferRequestMapper<R> mapper
  ) {
    var result = new LinkedHashMap<AbstractUserPreferences<?>, WalkProfileParameters<R>>();
    for (var profile : parameters.walk()) {
      var preferences = resolve(profile, mapper);
      var other = result.put(preferences, profile);
      if (other != null) {
        throw new IllegalArgumentException(
          "The transfer profiles '" +
            other.id() +
            "' and '" +
            profile.id() +
            "' have the same user preferences, the user preferences must be unique: " +
            preferences
        );
      }
    }
    return result;
  }
}
