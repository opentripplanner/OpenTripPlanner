package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model.basic.TransitMode.CARPOOL;
import static org.opentripplanner.transit.model.basic.TransitMode.TAXI;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.transit.model.basic.TransitMode;

class TransitModeMapperTest {

  static Stream<Arguments> testCases() {
    return Stream.of(
      Arguments.of(1500, TAXI),
      Arguments.of(1510, TAXI),
      Arguments.of(1551, CARPOOL),
      Arguments.of(1555, CARPOOL),
      Arguments.of(1560, CARPOOL),
      Arguments.of(1561, TAXI),
      Arguments.of(1580, TAXI)
    );
  }

  @ParameterizedTest(name = "{0} should map to {1}")
  @MethodSource("testCases")
  void map(int mode, TransitMode expectedMode) {
    assertEquals(expectedMode, TransitModeMapper.mapMode(mode));
  }
}
