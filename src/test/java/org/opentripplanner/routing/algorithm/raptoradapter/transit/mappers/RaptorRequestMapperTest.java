package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.routing.api.request.preference.Relax;

class RaptorRequestMapperTest {

  private static final Relax R1 = new Relax(1.0, 50);
  private static final Relax R2 = new Relax(1.5, 0);
  private static final Relax R3 = new Relax(2.0, 30);

  static List<Arguments> testCasesRelaxedCost() {
    return List.of(
      Arguments.of(Relax.NORMAL, 0, 0),
      Arguments.of(Relax.NORMAL, 10, 10),
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
  void mapRelaxCost(Relax input, int cost, int expected) {
    var calcCost = RaptorRequestMapper.mapRelaxCost(input);
    assertEquals(expected, calcCost.relax(cost));
  }
}
