package org.opentripplanner.ext.restapi.parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.api.parameter.ApiRequestMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class ApiRequestModeTest {

  static Stream<Arguments> testCases() {
    return Stream.of(
      Arguments.of(List.of(TransitMode.CARPOOL), ApiRequestMode.CARPOOL),
      Arguments.of(List.of(TransitMode.BUS), ApiRequestMode.BUS),
      Arguments.of(List.of(TransitMode.COACH), ApiRequestMode.COACH)
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void getTransitMode(List<TransitMode> transitModes, ApiRequestMode apiRequestMode) {
    assertEquals(transitModes, apiRequestMode.getTransitModes());
  }
}
