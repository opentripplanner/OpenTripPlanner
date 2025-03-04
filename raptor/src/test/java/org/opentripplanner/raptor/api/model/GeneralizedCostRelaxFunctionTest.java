package org.opentripplanner.raptor.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction.MAX_RATIO;
import static org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction.MAX_SLACK;
import static org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction.MIN_RATIO;
import static org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction.MIN_SLACK;

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

    assertThrows(IllegalArgumentException.class, () ->
      GeneralizedCostRelaxFunction.of(MIN_RATIO - 0.1)
    );
    assertThrows(IllegalArgumentException.class, () ->
      GeneralizedCostRelaxFunction.of(MAX_RATIO + 0.01)
    );
    assertThrows(IllegalArgumentException.class, () ->
      GeneralizedCostRelaxFunction.of(1, MIN_SLACK - 1)
    );
    assertThrows(IllegalArgumentException.class, () ->
      GeneralizedCostRelaxFunction.of(1, MAX_SLACK + 1)
    );
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
