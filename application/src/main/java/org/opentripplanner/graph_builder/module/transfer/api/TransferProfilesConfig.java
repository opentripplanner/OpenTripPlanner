package org.opentripplanner.graph_builder.module.transfer.api;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

/**
 * Parsed {@code transfers:} build-config block (see
 * {@code opentripplanner/OpenTripPlanner#7998}) - replaces {@code transferRequests} as the source
 * of profiles for the new raptor-data regular-transfer pipeline. Unrelated to, and parsed
 * independently of, the old {@code transferRequests}/{@code RegularTransferParameters} config,
 * which {@code DirectTransferGenerator} keeps reading unchanged for FLEX.
 *
 * @param defaultMaxDuration the default max duration for a profile's {@code maxDurations}, unless
 *                          overridden by at least one entry there
 * @param deduplicateDelta  cost-only tolerance used when deciding whether a dependent profile's
 *                          own path can be deduplicated against its {@code base}'s path (both
 *                          costed under the dependent's own preferences)
 * @param profiles          in the order declared in config - an unmatched request falls back to
 *                          {@code profiles().get(0)}. At least one profile is required.
 */
public record TransferProfilesConfig(
  Duration defaultMaxDuration,
  CostLinearFunction deduplicateDelta,
  List<TransferProfileConfig> profiles
) {
  public TransferProfilesConfig {
    if (profiles.isEmpty()) {
      throw new IllegalArgumentException(
        "At least one transfer profile must be configured under 'transfers'."
      );
    }
    profiles = List.copyOf(profiles);
  }

  public TransferProfileConfig fallbackProfile() {
    return profiles.get(0);
  }
}
