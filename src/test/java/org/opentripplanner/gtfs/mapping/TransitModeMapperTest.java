package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.transit.model.basic.TransitMode.CARPOOL;

import org.junit.jupiter.api.Test;

class TransitModeMapperTest {

  @Test
  void carpool() {
    assertEquals(CARPOOL, TransitModeMapper.mapMode(1700));
  }
}
