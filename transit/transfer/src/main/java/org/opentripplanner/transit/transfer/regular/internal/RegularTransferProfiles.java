package org.opentripplanner.transit.transfer.regular.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.transit.transfer.regular.RaptorTransferProfile;
import org.opentripplanner.transit.transfer.regular.spi.RegularTransferParameters;

/**
 * A validated, dependency-ordered collection of transfer profiles.
 * <p>
 * {@link #of} rejects a duplicate {@code profileId}, and a {@code deduplicationProfile}
 * referencing a profile that isn't part of the collection, before ordering the profiles so a
 * profile with a {@code deduplicationProfile} always comes after the profile it deduplicates
 * paths against. A circular {@code deduplicationProfile} reference is rejected too.
 * <p>
 * Simple, not efficient - a fixed-point iteration over the (well under 20) profiles this ever
 * holds, not a proper topological sort.
 */
final class RegularTransferProfiles<U> {

  private final List<RegularTransferParameters<U>> orderedProfiles;

  private RegularTransferProfiles(List<RegularTransferParameters<U>> orderedProfiles) {
    this.orderedProfiles = orderedProfiles;
  }

  static <U> RegularTransferProfiles<U> of(Collection<RegularTransferParameters<U>> profiles) {
    validateNoDuplicateProfileId(profiles);
    validateDeduplicationProfileReferencesExist(profiles);
    return new RegularTransferProfiles<>(order(profiles));
  }

  List<RegularTransferParameters<U>> orderedProfiles() {
    return orderedProfiles;
  }

  private static <U> void validateNoDuplicateProfileId(
    Collection<RegularTransferParameters<U>> profiles
  ) {
    Map<RaptorTransferProfile, RegularTransferParameters<U>> byProfileId = new EnumMap<>(
      RaptorTransferProfile.class
    );
    for (var profile : profiles) {
      if (byProfileId.put(profile.profileId(), profile) != null) {
        throw new IllegalArgumentException(
          "Duplicate transfer profile for " + profile.profileId()
        );
      }
    }
  }

  private static <U> void validateDeduplicationProfileReferencesExist(
    Collection<RegularTransferParameters<U>> profiles
  ) {
    var profileIds = profiles.stream().map(RegularTransferParameters::profileId).toList();
    for (var profile : profiles) {
      var deduplicationProfile = profile.deduplicationProfile();
      if (deduplicationProfile != null && !profileIds.contains(deduplicationProfile)) {
        throw new IllegalArgumentException(
          "Transfer profile " +
          profile.profileId() +
          " has deduplicationProfile " +
          deduplicationProfile +
          ", which is not a configured transfer profile."
        );
      }
    }
  }

  /**
   * Fixed-point iteration: seed with every profile that has no {@code deduplicationProfile}, then
   * repeatedly add any remaining profile whose {@code deduplicationProfile} has already been
   * placed, until every profile is placed. Callers must have already validated that every
   * reference points at a configured profile (see {@link #of}), so a pass that adds nothing can
   * only mean a cycle among what's left.
   */
  private static <U> List<RegularTransferParameters<U>> order(
    Collection<RegularTransferParameters<U>> profiles
  ) {
    var remaining = new ArrayList<RegularTransferParameters<U>>(profiles);
    var results = new ArrayList<RegularTransferParameters<U>>(
      profiles.stream().filter(p -> p.deduplicationProfile() == null).toList()
    );
    remaining.removeAll(results);

    while (results.size() != profiles.size()) {
      var added = remaining.stream().filter(p -> isDeduplicationProfilePresent(p, results)).toList();
      if (added.isEmpty()) {
        throw new IllegalArgumentException(
          "Circular deduplicationProfile reference involving transfer profile(s) " + remaining
        );
      }
      results.addAll(added);
      remaining.removeAll(added);
    }
    return results;
  }

  private static <U> boolean isDeduplicationProfilePresent(
    RegularTransferParameters<U> p,
    List<RegularTransferParameters<U>> list
  ) {
    return list.stream().anyMatch(it -> it.profileId() == p.deduplicationProfile());
  }
}
