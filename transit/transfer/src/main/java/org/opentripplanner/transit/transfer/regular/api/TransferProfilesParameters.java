package org.opentripplanner.transit.transfer.regular.api;

import java.util.HashSet;
import java.util.List;

/**
 * The configuration of the regular transfers: the profiles to generate transfer paths for. Only
 * walk profiles are supported.
 *
 * @param walk the walk profiles, in the order declared in the configuration. At least one is
 *             required, and no two may have equal user preferences, since the user preferences
 *             identify a profile.
 * @param <R>  the request type, opaque to the module
 */
public record TransferProfilesParameters<R>(List<WalkProfileParameters<R>> walk) {
  public TransferProfilesParameters {
    walk = List.copyOf(walk);
    if (walk.isEmpty()) {
      throw new IllegalArgumentException("At least one walk transfer profile is required.");
    }
    var preferences = new HashSet<WalkPreferences>();
    for (var profile : walk) {
      if (!preferences.add(profile.userPreferences())) {
        throw new IllegalArgumentException(
          "Duplicate walk transfer profile, the user preferences must be unique: " +
            profile.userPreferences()
        );
      }
    }
  }
}
