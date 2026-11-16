package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;

/**
 * Finds the profile whose stored paths the Raptor transfers for some user preferences are created
 * from: the profile with the closest user preferences, see
 * {@link OverlaidTemplatePaths#closestProfile}. A warmup entry is matched like any other
 * preferences, so the transfers created for it when the server starts are made from the same paths
 * as a request with these preferences gets.
 * <p>
 * This class is immutable and thread-safe.
 */
final class ProfileMatcher {

  private final Comparator<AbstractUserPreferences<?>> declarationOrder;

  /**
   * If two profiles are the same distance from the user preferences, the one that is declared first
   * in the configuration is chosen.
   *
   * @param declaredProfiles the user preferences of the profiles in the order they are declared in
   */
  ProfileMatcher(List<AbstractUserPreferences<?>> declaredProfiles) {
    var order = new HashMap<AbstractUserPreferences<?>, Integer>();
    for (var profile : declaredProfiles) {
      order.putIfAbsent(profile, order.size());
    }
    this.declarationOrder = Comparator.comparingInt(p -> order.getOrDefault(p, Integer.MAX_VALUE));
  }

  /**
   * The profile the paths for {@code userPreferences} are read from: the profile with the closest
   * user preferences, {@code null} if there are no paths.
   */
  @Nullable
  AbstractUserPreferences<?> closestProfile(
    OverlaidTemplatePaths<?> paths,
    AbstractUserPreferences<?> userPreferences
  ) {
    return paths.closestProfile(userPreferences, declarationOrder);
  }
}
