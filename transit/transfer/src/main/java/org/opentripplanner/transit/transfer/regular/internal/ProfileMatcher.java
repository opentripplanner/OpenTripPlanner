package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
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
  static final ProfileMatcher NO_WARMUP = new ProfileMatcher(Map.of(), Set.of());

  private final Map<AbstractUserPreferences<?>, AbstractUserPreferences<?>> parentsOfWarmup;
  private final Set<TransferProfileType> fallbackToClosestType;

  /**
   * @param parentsOfWarmup       the user preferences of the parent profile, by the user
   *                              preferences of each warmup entry
   * @param fallbackToClosestType the types that use the closest profile of another type if there is
   *                              no profile of the type
   */
  ProfileMatcher(
    Map<AbstractUserPreferences<?>, AbstractUserPreferences<?>> parentsOfWarmup,
    Set<TransferProfileType> fallbackToClosestType
  ) {
    this.parentsOfWarmup = Map.copyOf(parentsOfWarmup);
    this.fallbackToClosestType = Set.copyOf(fallbackToClosestType);
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
    var profile = paths.closestProfile(
      userPreferences,
      fallbackToClosestType.contains(userPreferences.type())
    );
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
      // A profile wins a tie
      if (distance < bestDistance) {
        bestDistance = distance;
        profile = warmup.getValue();
      }
    }
    return profile;
  }
}
