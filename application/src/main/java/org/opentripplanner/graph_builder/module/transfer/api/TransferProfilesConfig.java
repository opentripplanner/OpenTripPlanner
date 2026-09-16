package org.opentripplanner.graph_builder.module.transfer.api;

import java.time.Duration;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.transit.transfer.regular.RaptorTransferProfile;

/**
 * Parsed {@code transfers:} build-config block (see
 * {@code opentripplanner/OpenTripPlanner#7998}) - replaces {@code transferRequests} as the source
 * of profiles for the new raptor-data regular-transfer pipeline. Unrelated to, and parsed
 * independently of, the old {@code transferRequests}/{@code RegularTransferParameters} config,
 * which {@code DirectTransferGenerator} keeps reading unchanged for FLEX.
 *
 * @param defaultMaxDuration  the default max duration for a profile's {@code maxDurations},
 *                            unless overridden by at least one entry there
 * @param deduplicateDelta    cost-only tolerance used when deciding whether a dependent
 *                            profile's own path can be deduplicated against its
 *                            {@code deduplicationProfile}'s path (both costed under the
 *                            dependent's own preferences)
 * @param fallbackProfileId   the profile a request that matches no configured profile should
 *                            use, or {@code null} to fall back to {@code profiles().get(0)}
 *                            (the first profile declared in config). Must reference a
 *                            configured profile when set.
 * @param profiles            in the order declared in config. At least one profile is required.
 */
public record TransferProfilesConfig(
  Duration defaultMaxDuration,
  CostLinearFunction deduplicateDelta,
  @Nullable RaptorTransferProfile fallbackProfileId,
  List<TransferProfileConfig> profiles
) {
  public TransferProfilesConfig {
    if (profiles.isEmpty()) {
      throw new IllegalArgumentException(
        "At least one transfer profile must be configured under 'transfers'."
      );
    }
    profiles = List.copyOf(profiles);
    if (
      fallbackProfileId != null &&
      profiles.stream().noneMatch(p -> p.profileId() == fallbackProfileId)
    ) {
      throw new IllegalArgumentException(
        "The 'fallbackProfile' (" +
          fallbackProfileId +
          ") must reference a configured transfer profile."
      );
    }
  }

  public TransferProfileConfig fallbackProfile() {
    if (fallbackProfileId == null) {
      return profiles.get(0);
    }
    return profiles
      .stream()
      .filter(p -> p.profileId() == fallbackProfileId)
      .findFirst()
      .orElseThrow();
  }
}
