package org.opentripplanner.gbfs.network;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The per-network GBFS configuration shared by the vehicle rental graph builder and the vehicle
 * rental service directory, mapped from the {@code gbfs} section of {@code otp-config.json}.
 * <p>
 * Field inheritance is resolved when the configuration is parsed, so the parameters held here are
 * already complete: a listed network's entry has the {@code defaults} filled in for every field it
 * did not name.
 *
 * @param defaults the values applied to a network that is not listed
 * @param includeUnlistedNetworks whether a network present in the GBFS manifest but absent from
 *   {@code networks} is loaded at all. Kept separate from {@code defaults} so that adding defaults
 *   to avoid repetition cannot silently widen which networks OTP loads.
 * @param byNetwork the resolved parameters for each listed network, keyed by GBFS {@code system_id}
 */
public record GbfsNetworkOverrides(
  GbfsNetworkParameters defaults,
  boolean includeUnlistedNetworks,
  Map<String, GbfsNetworkParameters> byNetwork
) {
  public GbfsNetworkOverrides {
    Objects.requireNonNull(defaults);
    byNetwork = Map.copyOf(byNetwork);
  }

  /**
   * The configuration used when {@code otp-config.json} has no {@code gbfs} section: no network is
   * listed and unlisted networks are dropped, so nothing is loaded.
   */
  public static GbfsNetworkOverrides none() {
    return new GbfsNetworkOverrides(GbfsNetworkParameters.DEFAULT, false, Map.of());
  }

  /**
   * The parameters for the given GBFS {@code system_id}, or empty if the network is not configured
   * and should be skipped.
   */
  public Optional<GbfsNetworkParameters> forNetwork(String network) {
    var listed = byNetwork.get(network);
    if (listed != null) {
      return Optional.of(listed);
    }
    return includeUnlistedNetworks ? Optional.of(defaults) : Optional.empty();
  }
}
