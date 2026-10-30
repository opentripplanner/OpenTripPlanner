package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;

/**
 * Finds the profile whose stored paths the Raptor transfers for some user preferences are created
 * from: the profile with the closest user preferences, see
 * {@link OverlaidTemplatePaths#closestProfile}.
 * <p>
 * A warmup entry has no paths of its own, but it takes part in the matching. If the preferences
 * are closer to a warmup entry than to any profile, the paths of the parent of the entry are used.
 * This way the Raptor transfers created for the preferences of a warmup entry, when the server
 * starts, are the ones a request with these preferences gets.
 * <p>
 * This class is immutable and thread-safe.
 */
final class ProfileMatcher {

  /** No warmup entries: the closest profile. */
  static final ProfileMatcher NO_WARMUP = new ProfileMatcher(Map.of(), List.of());

  private final Map<AbstractUserPreferences<?>, AbstractUserPreferences<?>> parentsOfWarmup;
  private final Comparator<AbstractUserPreferences<?>> declarationOrder;

  /**
   * If two profiles, or two warmup entries, are the same distance from the user preferences, the
   * one that is declared first in the configuration is chosen. A profile wins a tie against a warmup
   * entry.
   *
   * @param parentsOfWarmup  the user preferences of the parent profile, by the user preferences of
   *                         each warmup entry, in the order the entries are declared in
   * @param declaredProfiles the user preferences of the profiles in the order they are declared in
   */
  ProfileMatcher(
    Map<AbstractUserPreferences<?>, AbstractUserPreferences<?>> parentsOfWarmup,
    List<AbstractUserPreferences<?>> declaredProfiles
  ) {
    // Keep the order of the entries, Map.copyOf has a different order from run to run
    this.parentsOfWarmup = Collections.unmodifiableMap(new LinkedHashMap<>(parentsOfWarmup));
    var order = new HashMap<AbstractUserPreferences<?>, Integer>();
    for (var profile : declaredProfiles) {
      order.putIfAbsent(profile, order.size());
    }
    this.declarationOrder = Comparator.comparingInt(p -> order.getOrDefault(p, Integer.MAX_VALUE));
  }

  /**
   * The profile the paths for {@code userPreferences} are read from. {@code null} if there are no
   * paths.
   */
  @Nullable
  AbstractUserPreferences<?> closestProfile(
    OverlaidTemplatePaths<?> paths,
    AbstractUserPreferences<?> userPreferences
  ) {
    var profile = paths.closestProfile(userPreferences, declarationOrder);
    if (parentsOfWarmup.isEmpty() || userPreferences.equals(profile)) {
      return profile;
    }
    double bestDistance =
      profile == null ? Double.MAX_VALUE : userPreferences.normalizedDistance(profile);
    for (var warmup : parentsOfWarmup.entrySet()) {
      if (
        !paths.hasProfile(warmup.getValue()) || warmup.getKey().type() != userPreferences.type()
      ) {
        continue;
      }
      double distance = userPreferences.normalizedDistance(warmup.getKey());
      // A profile wins a tie, and the first declared warmup entry wins a tie between entries
      if (distance < bestDistance - TemplatePathsByUserProfile.DISTANCE_EPSILON) {
        bestDistance = distance;
        profile = warmup.getValue();
      }
    }
    return profile;
  }
}
