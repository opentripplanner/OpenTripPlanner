package org.opentripplanner.ext.trias.mapping;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.transit.model.basic.TransitMode;

class PtModeMapperTest {

  @ParameterizedTest
  @EnumSource(TransitMode.class)
  void map(TransitMode mode) {
    assertNotNull(PtModeMapper.map(mode));
  }
}
