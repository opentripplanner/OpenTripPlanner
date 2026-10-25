package org.opentripplanner.transit.transfer.regular.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfilesParameters;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;

/**
 * The user preferences of the configured profiles. A profile without preferences of its own gets
 * the preferences of its routing request.
 */
public final class ProfileUserPreferences {

  private ProfileUserPreferences() {}

  /** The user preferences of the profile: its own, or else the ones of its routing request. */
  static <R> AbstractUserPreferences<?> resolve(
    TransferProfileParameters<R> profile,
    TransferRequestMapper<R> mapper
  ) {
    var own = profile.userPreferences();
    return own != null ? own : mapper.toUserPreferences(profile.routingRequest());
  }

  /**
   * The profiles by their user preferences, in the order they are declared. The user preferences
   * identify a profile in the repository, so two profiles with equal preferences are rejected.
   */
  public static <R> Map<AbstractUserPreferences<?>, TransferProfileParameters<R>> resolveAll(
    TransferProfilesParameters<R> parameters,
    TransferRequestMapper<R> mapper
  ) {
    var result = new LinkedHashMap<AbstractUserPreferences<?>, TransferProfileParameters<R>>();
    var idsByPreferences = new HashMap<AbstractUserPreferences<?>, String>();
    for (var profile : parameters.profiles()) {
      var preferences = resolve(profile, mapper);
      result.put(preferences, profile);
      assertUnique(idsByPreferences.put(preferences, profile.id()), profile.id(), preferences);
    }
    for (var entry : parameters.warmup()) {
      assertUnique(
        idsByPreferences.put(entry.userPreferences(), entry.id()),
        entry.id(),
        entry.userPreferences()
      );
    }
    return result;
  }

  /**
   * The warmup entries by their user preferences, with the user preferences of the parent profile
   * of each entry.
   */
  static <R> Map<AbstractUserPreferences<?>, AbstractUserPreferences<?>> warmupParents(
    TransferProfilesParameters<R> parameters,
    TransferRequestMapper<R> mapper
  ) {
    var preferencesById = new HashMap<String, AbstractUserPreferences<?>>();
    for (var profile : parameters.profiles()) {
      preferencesById.put(profile.id(), resolve(profile, mapper));
    }
    var result = new LinkedHashMap<AbstractUserPreferences<?>, AbstractUserPreferences<?>>();
    for (var entry : parameters.warmup()) {
      result.put(entry.userPreferences(), preferencesById.get(entry.parent()));
    }
    return result;
  }

  private static void assertUnique(
    @Nullable String otherId,
    String id,
    AbstractUserPreferences<?> preferences
  ) {
    if (otherId != null) {
      throw new IllegalArgumentException(
        "The transfer profiles '" +
          otherId +
          "' and '" +
          id +
          "' have the same user preferences, the user preferences must be unique: " +
          preferences
      );
    }
  }
}
