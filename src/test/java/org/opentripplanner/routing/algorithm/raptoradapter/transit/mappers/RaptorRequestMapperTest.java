package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;

class RaptorRequestMapperTest {

  private static final CostLinearFunction R1 = CostLinearFunction.of("50 + 1.0x");
  private static final CostLinearFunction R2 = CostLinearFunction.of("0 + 1.5x");
  private static final CostLinearFunction R3 = CostLinearFunction.of("30 + 2.0x");

  static List<Arguments> testCasesRelaxedCost() {
    return List.of(
      Arguments.of(CostLinearFunction.NORMAL, 0, 0),
      Arguments.of(CostLinearFunction.NORMAL, 10, 10),
      Arguments.of(R1, 0, 5000),
      Arguments.of(R1, 7, 5007),
      Arguments.of(R2, 0, 0),
      Arguments.of(R2, 100, 150),
      Arguments.of(R3, 0, 3000),
      Arguments.of(R3, 100, 3200)
    );
  }

  @ParameterizedTest
  @MethodSource("testCasesRelaxedCost")
  void mapRelaxCost(CostLinearFunction input, int cost, int expected) {
    var calcCost = RaptorRequestMapper.mapRelaxCost(input);
    assertEquals(expected, calcCost.relax(cost));
  }
}
