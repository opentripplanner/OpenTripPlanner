package org.opentripplanner.transit.transfer.regular.internal;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;

/**
 * Finds the profile whose stored paths the Raptor transfers for some user preferences are created
 * from: the profile with the closest user preferences. A warmup entry is matched like any other
 * preferences, so the transfers created for it when the server starts are made from the same paths
 * as a request with these preferences gets.
 * <p>
 * The profiles are the ones of the transfer configuration the server starts with. They are
 * expected to be the profiles paths were stored for at graph build.
 * <p>
 * This class is immutable and thread-safe.
 */
final class ProfileMatcher {

  /**
   * Distances within this limit are equal. The distance is a sum of floating point numbers, so
   * two distances that are the same by the numbers can differ in the last digits.
   */
  static final double DISTANCE_EPSILON = 1e-9;

  /** The profiles in the order they are declared in, without duplicates. */
  private final List<AbstractUserPreferences<?>> profiles;

  /**
   * If two profiles are the same distance from the user preferences, the one that is declared first
   * in the configuration is chosen.
   *
   * @param declaredProfiles the user preferences of the profiles in the order they are declared in
   */
  ProfileMatcher(List<AbstractUserPreferences<?>> declaredProfiles) {
    this.profiles = declaredProfiles.stream().distinct().toList();
  }

  /**
   * The profile closest to the given user preferences: the preferences themselves if they are a
   * profile, otherwise the profile of the same type with the smallest
   * {@link AbstractUserPreferences#normalizedDistance}. {@code null} if there are no profiles.
   *
   * @throws IllegalArgumentException if there are profiles, but not of the type of the preferences.
   *                                  A request can only use the transfers of a configured type.
   */
  @Nullable
  AbstractUserPreferences<?> closestProfile(AbstractUserPreferences<?> userPreferences) {
    if (profiles.contains(userPreferences)) {
      return userPreferences;
    }
    if (profiles.isEmpty()) {
      return null;
    }
    var candidates = profiles
      .stream()
      .filter(profile -> profile.type() == userPreferences.type())
      .toList();
    if (candidates.isEmpty()) {
      throw new IllegalArgumentException(
        "There is no transfer profile of the type " +
          userPreferences.type() +
          ", the transfers of that type are not configured."
      );
    }
    AbstractUserPreferences<?> best = null;
    double bestDistance = Double.MAX_VALUE;
    for (var candidate : candidates) {
      double distance = userPreferences.normalizedDistance(candidate);
      // A candidate that is declared first wins a tie
      if (best == null || distance < bestDistance - DISTANCE_EPSILON) {
        best = candidate;
        bestDistance = distance;
      }
    }
    return best;
  }
}
