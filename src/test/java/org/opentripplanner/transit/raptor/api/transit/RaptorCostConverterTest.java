package org.opentripplanner.transit.raptor.api.transit;

import org.junit.Test;

import static org.junit.Assert.*;

public class RaptorCostConverterTest {
  @Test
  public void toOtpDomainCost() {
    assertEquals(0, RaptorCostConverter.toOtpDomainCost(49));
    assertEquals(1, RaptorCostConverter.toOtpDomainCost(50));
    assertEquals(300, RaptorCostConverter.toOtpDomainCost(30_000));
  }

  @Test
  public void toRaptorCost() {
    assertEquals(0, RaptorCostConverter.toRaptorCost(0));
    assertEquals(100, RaptorCostConverter.toRaptorCost(1));
    assertEquals(3_000, RaptorCostConverter.toRaptorCost(30));
  }
}