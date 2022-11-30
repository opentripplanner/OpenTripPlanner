package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.transit.model.basic.TransitMode;

class TransitModeMapperTest {

  static Stream<Arguments> testCases = Stream.of(
    Arguments.of(1500, TransitMode.TAXI),
    Arguments.of(1510, TransitMode.TAXI),
    Arguments.of(1550, TransitMode.CARPOOL),
    Arguments.of(1551, TransitMode.CARPOOL),
    Arguments.of(1560, TransitMode.CARPOOL)
  );

  @ParameterizedTest(name = "{0} should map to {1}")
  @VariableSource("testCases")
  void map(int mode, TransitMode expectedMode) {
    assertEquals(expectedMode, TransitModeMapper.mapMode(mode));
  }
}
