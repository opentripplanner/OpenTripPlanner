package org.opentripplanner.gbfs.network;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.gbfs.network.GeofencingZonePhase.OFF;
import static org.opentripplanner.gbfs.network.GeofencingZonePhase.PERMANENT;

import java.util.Map;
import org.junit.jupiter.api.Test;

class GbfsNetworkOverridesTest {

  private static final GbfsNetworkParameters DEFAULTS = new GbfsNetworkParameters(OFF, true, false);
  private static final GbfsNetworkParameters TIER = new GbfsNetworkParameters(
    PERMANENT,
    false,
    true
  );

  @Test
  void listedNetworkResolvesToItsOwnParameters() {
    var subject = new GbfsNetworkOverrides(DEFAULTS, false, Map.of("tier", TIER));

    assertThat(subject.forNetwork("tier")).hasValue(TIER);
  }

  @Test
  void unlistedNetworkIsDroppedWhenUnlistedNetworksAreExcluded() {
    var subject = new GbfsNetworkOverrides(DEFAULTS, false, Map.of("tier", TIER));

    assertThat(subject.forNetwork("ryde")).isEmpty();
  }

  @Test
  void unlistedNetworkResolvesToDefaultsWhenUnlistedNetworksAreIncluded() {
    var subject = new GbfsNetworkOverrides(DEFAULTS, true, Map.of("tier", TIER));

    assertThat(subject.forNetwork("ryde")).hasValue(DEFAULTS);
  }
}
