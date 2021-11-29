package org.opentripplanner.transit.raptor.api.transit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opentripplanner.routing.algorithm.raptor.transit.cost.RaptorCostConverter;

public class RaptorCostConverterTest {
  @Test
  public void toOtpDomainCost() {
    assertEquals(-1, RaptorCostConverter.toOtpDomainCost(-1));
    assertEquals(0, RaptorCostConverter.toOtpDomainCost(49));
    assertEquals(1, RaptorCostConverter.toOtpDomainCost(50));
    assertEquals(300, RaptorCostConverter.toOtpDomainCost(30_000));
  }

  @Test
  public void toRaptorCost() {
    assertEquals(-1, RaptorCostConverter.toOtpDomainCost(-1));
    assertEquals(0, RaptorCostConverter.toRaptorCost(0));
    assertEquals(100, RaptorCostConverter.toRaptorCost(1));
    assertEquals(3_000, RaptorCostConverter.toRaptorCost(30));
  }

  @Test
  public void toRaptorCosts() {
    int[] expected = {100, 80};
    int[] result = RaptorCostConverter.toRaptorCosts(new double[]{1.0, 0.8});
    assertArrayEquals(expected, result);
  }

  @Test
  public void testToString() {
    assertEquals("$120", RaptorCostConverter.toString(12_000));
  }
}