package org.opentripplanner.apis.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.network.BikeAccess;

class BikesAllowedMapperTest {

  @Test
  void mapping() {
    assertThat(BikeAccess.ALLOWED.toString())
      .isEqualTo(BikesAllowedMapper.map(BikeAccess.ALLOWED).toString());
    assertThat(BikeAccess.NOT_ALLOWED.toString())
      .isEqualTo(BikesAllowedMapper.map(BikeAccess.NOT_ALLOWED).toString());
    assertThat("NO_INFORMATION").isEqualTo(BikesAllowedMapper.map(BikeAccess.UNKNOWN).toString());
  }
}
