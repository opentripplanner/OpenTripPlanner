package org.opentripplanner.apis.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.network.CarAccess;

class CarsAllowedMapperTest {

  @Test
  void mapping() {
    Arrays
      .stream(CarAccess.values())
      .filter(ba -> ba != CarAccess.UNKNOWN)
      .forEach(d -> {
        var mapped = CarsAllowedMapper.map(d);
        assertEquals(d.toString(), mapped.toString());
      });
  }
}
