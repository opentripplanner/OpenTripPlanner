package org.opentripplanner.raptor.rangeraptor.internalapi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class NoopPassThroughPointsServiceTest {

  @Test
  void isNoop() {
    assertTrue(PassThroughPointsService.NOOP.isNoop());
  }

  @Test
  void isPassThroughPoint() {
    assertFalse(PassThroughPointsService.NOOP.isPassThroughPoint(1));
  }

  @Test
  void updateC2Value() {
    assertThrows(UnsupportedOperationException.class, () ->
      PassThroughPointsService.NOOP.updateC2Value(1000, i -> fail())
    );
  }

  @Test
  void dominanceFunction() {
    assertThrows(
      UnsupportedOperationException.class,
      PassThroughPointsService.NOOP::dominanceFunction
    );
  }

  @Test
  void acceptC2AtDestination() {
    assertThrows(
      UnsupportedOperationException.class,
      PassThroughPointsService.NOOP::acceptC2AtDestination
    );
  }
}
