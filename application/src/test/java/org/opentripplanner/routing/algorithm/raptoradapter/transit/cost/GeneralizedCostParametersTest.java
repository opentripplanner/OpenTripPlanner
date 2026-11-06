package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.BitSet;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.CostLinearFunction;

class GeneralizedCostParametersTest {

  private final GeneralizedCostParameters subject = builder().build();

  @Test
  void testEqualsAndHashCode() {
    var same = builder().build();
    assertEquals(subject, same);
    assertEquals(subject.hashCode(), same.hashCode());

    assertNotEquals(
      subject,
      builder()
        .transitReluctanceFactors(new double[] { 1.0, 3.0 })
        .build()
    );
    assertNotEquals(subject, builder().wheelchairEnabled(true).build());
    assertNotEquals(subject, builder().unpreferredPatterns(bitSet(3)).build());
    assertNotEquals(
      subject,
      builder().unpreferredCost(CostLinearFunction.of("10 + 2.0 t")).build()
    );
  }

  private static GeneralizedCostParametersBuilder builder() {
    return GeneralizedCostParameters.of().transitReluctanceFactors(new double[] { 1.0, 2.0 });
  }

  private static BitSet bitSet(int index) {
    var bitSet = new BitSet();
    bitSet.set(index);
    return bitSet;
  }
}
