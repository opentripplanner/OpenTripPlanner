package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AccessEgressTypeTest {

  @Test
  void isAccess() {
    assertTrue(AccessEgressType.ACCESS.isAccess());
    assertFalse(AccessEgressType.EGRESS.isAccess());
  }

  @Test
  void isEgress() {
    assertTrue(AccessEgressType.EGRESS.isEgress());
    assertFalse(AccessEgressType.ACCESS.isEgress());
  }
}
