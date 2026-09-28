package org.opentripplanner.raptor.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction.SLACK_RANGE;

import org.junit.jupiter.api.Test;

class GeneralizedCostRelaxFunctionTest {

  @Test
  void relaxCost() {
    assertEquals(100, GeneralizedCostRelaxFunction.of(1.0).relax(100));
    assertEquals(75, GeneralizedCostRelaxFunction.of(1.5).relax(50));
    assertEquals(100, GeneralizedCostRelaxFunction.of(1.0, 50).relax(50));
    assertEquals(150, GeneralizedCostRelaxFunction.of(1.25, 25).relax(100));

    // The values below is only ~= to the expected value, this is because of the RATIO_RESOLUTION
    // optimization. The function is accurate enough, but most important consistent.
    assertEquals(10117, GeneralizedCostRelaxFunction.of(1.01, 0).relax(10_000));
    assertEquals(10195, GeneralizedCostRelaxFunction.of(1.02, 0).relax(10_000));
    assertEquals(10312, GeneralizedCostRelaxFunction.of(1.03, 0).relax(10_000));
    assertEquals(10390, GeneralizedCostRelaxFunction.of(1.04, 0).relax(10_000));
    assertEquals(10507, GeneralizedCostRelaxFunction.of(1.05, 0).relax(10_000));
    assertEquals(10585, GeneralizedCostRelaxFunction.of(1.06, 0).relax(10_000));
    assertEquals(10703, GeneralizedCostRelaxFunction.of(1.07, 0).relax(10_000));

    var ex = assertThrows(IllegalArgumentException.class, () ->
      GeneralizedCostRelaxFunction.of(0.99)
    );
    assertEquals("Cost ratio is not in range: 0.99 not in [1.0, 4.0)", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () -> GeneralizedCostRelaxFunction.of(4.0));
    assertEquals("Cost ratio is not in range: 4.00 not in [1.0, 4.0)", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () ->
      GeneralizedCostRelaxFunction.of(1.0, SLACK_RANGE.startInclusive() - 1)
    );
    assertEquals("Cost slack is not in range: -1 not in [0s, 4h]", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () ->
      GeneralizedCostRelaxFunction.of(1.0, SLACK_RANGE.endInclusive() + 1)
    );
    assertEquals("Cost slack is not in range: 1440001 not in [0s, 4h]", ex.getMessage());
  }

  @Test
  void testToString() {
    assertEquals("f(x) = 1.10 * x + 12.0", GeneralizedCostRelaxFunction.of(1.1, 1200).toString());
  }

  @Test
  void testEqualsAndHashCode() {
    var a = GeneralizedCostRelaxFunction.of(1.0, 12);
    var same = GeneralizedCostRelaxFunction.of(1.0, 12);
    var diffRatio = GeneralizedCostRelaxFunction.of(3.9, 12);
    var diffSlack = GeneralizedCostRelaxFunction.of(1.0, 13);

    assertEquals(a, a);
    assertEquals(a, same);
    assertEquals(a.hashCode(), same.hashCode());
    assertNotEquals(a, diffRatio);
    assertNotEquals(a.hashCode(), diffRatio.hashCode());
    assertNotEquals(a, diffSlack);
    assertNotEquals(a.hashCode(), diffSlack.hashCode());
  }
}
