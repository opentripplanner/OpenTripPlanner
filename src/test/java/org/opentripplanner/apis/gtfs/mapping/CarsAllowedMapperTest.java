package org.opentripplanner.apis.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.network.CarAccess;

class CarsAllowedMapperTest {

  @Test
  void mapping() {
    assertThat(CarAccess.ALLOWED.toString())
      .isEqualTo(CarsAllowedMapper.map(CarAccess.ALLOWED).toString());
    assertThat(CarAccess.NOT_ALLOWED.toString())
      .isEqualTo(CarsAllowedMapper.map(CarAccess.NOT_ALLOWED).toString());
    assertThat("NO_INFORMATION").isEqualTo(CarsAllowedMapper.map(CarAccess.UNKNOWN).toString());
  }
}
