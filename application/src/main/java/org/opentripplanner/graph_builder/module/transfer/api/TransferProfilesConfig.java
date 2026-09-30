package org.opentripplanner.graph_builder.module.transfer.api;

import java.util.List;

/**
 * Parsed {@code transfers:} build-config block (see
 * {@code opentripplanner/OpenTripPlanner#7998}) - replaces {@code transferRequests} as the source
 * of profiles for the new raptor-data regular-transfer pipeline. Unrelated to, and parsed
 * independently of, the old {@code transferRequests}/{@code RegularTransferParameters} config,
 * which {@code FlexTransferGenerator} keeps reading unchanged for FLEX.
 * <p>
 * Only {@code WALK} is supported for now (see {@code TransferProfileType}) - {@code profiles}
 * stays a list for forward compatibility with additional profiles later.
 *
 * @param profiles in the order declared in config. At least one profile is required.
 */
public record TransferProfilesConfig(List<TransferProfileConfig> profiles) {
  public TransferProfilesConfig {
    if (profiles.isEmpty()) {
      throw new IllegalArgumentException(
        "At least one transfer profile must be configured under 'transfers'."
      );
    }
    profiles = List.copyOf(profiles);
  }
}
