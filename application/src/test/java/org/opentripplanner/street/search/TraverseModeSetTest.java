package org.opentripplanner.street.search;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TraverseModeSetTest {

  @Test
  public void testCarMode() {
    TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.CAR);

    assertTrue(modeSet.getCar());
    assertFalse(modeSet.getWalk());
    assertFalse(modeSet.getBicycle());
  }

  @Test
  public void testWalkMode() {
    TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.WALK);

    assertTrue(modeSet.getWalk());
    assertFalse(modeSet.getCar());
    assertFalse(modeSet.getBicycle());
  }

  @Test
  public void testBikeMode() {
    TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.BICYCLE);

    assertTrue(modeSet.getBicycle());
    assertFalse(modeSet.getWalk());
    assertFalse(modeSet.getCar());
    assertFalse(modeSet.getWalk());
  }
}
