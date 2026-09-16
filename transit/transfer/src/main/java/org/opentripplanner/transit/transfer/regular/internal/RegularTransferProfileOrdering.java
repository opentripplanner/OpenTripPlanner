package org.opentripplanner.transit.transfer.regular.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.transit.transfer.regular.RaptorTransferProfile;
import org.opentripplanner.transit.transfer.regular.spi.RegularTransferParameters;

/**
 * Orders transfer profiles so a profile with a {@code deduplicationProfile} always comes after
 * the profile it deduplicates paths against, and rejects a configuration where a
 * {@code deduplicationProfile} reference is missing or the references form a cycle.
 */
final class RegularTransferProfileOrdering {

  private RegularTransferProfileOrdering() {}

  static <U> List<RegularTransferParameters<U>> order(
    Collection<RegularTransferParameters<U>> profiles
  ) {
    Map<RaptorTransferProfile, RegularTransferParameters<U>> byProfileId = new EnumMap<>(
      RaptorTransferProfile.class
    );
    for (var profile : profiles) {
      if (byProfileId.put(profile.profileId(), profile) != null) {
        throw new IllegalArgumentException("Duplicate transfer profile for " + profile.profileId());
      }
    }
    for (var profile : profiles) {
      var deduplicationProfile = profile.deduplicationProfile();
      if (deduplicationProfile != null && !byProfileId.containsKey(deduplicationProfile)) {
        throw new IllegalArgumentException(
          "Transfer profile " +
            profile.profileId() +
            " has deduplicationProfile " +
            deduplicationProfile +
            ", which is not a configured transfer profile."
        );
      }
    }

    List<RegularTransferParameters<U>> ordered = new ArrayList<>(byProfileId.size());
    Set<RaptorTransferProfile> visited = EnumSet.noneOf(RaptorTransferProfile.class);
    Set<RaptorTransferProfile> visiting = EnumSet.noneOf(RaptorTransferProfile.class);

    for (var profileId : byProfileId.keySet()) {
      visit(profileId, byProfileId, visited, visiting, ordered);
    }
    return ordered;
  }

  private static <U> void visit(
    RaptorTransferProfile profileId,
    Map<RaptorTransferProfile, RegularTransferParameters<U>> byProfileId,
    Set<RaptorTransferProfile> visited,
    Set<RaptorTransferProfile> visiting,
    List<RegularTransferParameters<U>> ordered
  ) {
    if (visited.contains(profileId)) {
      return;
    }
    if (!visiting.add(profileId)) {
      throw new IllegalArgumentException(
        "Circular deduplicationProfile reference involving transfer profile " + profileId
      );
    }
    var profile = byProfileId.get(profileId);
    var deduplicationProfile = profile.deduplicationProfile();
    if (deduplicationProfile != null) {
      visit(deduplicationProfile, byProfileId, visited, visiting, ordered);
    }
    visiting.remove(profileId);
    visited.add(profileId);
    ordered.add(profile);
  }
}
