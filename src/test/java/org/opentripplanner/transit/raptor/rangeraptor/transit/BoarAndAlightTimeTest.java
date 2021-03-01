package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BoarAndAlightTimeTest {

  @Test
  public void testToString() {
    assertEquals("(00:00:30, 00:00:40)", new BoarAndAlightTime(30, 40).toString());
  }
}