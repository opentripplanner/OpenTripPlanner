package org.opentripplanner.raptor.util.paretoset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor.util.paretoset.ParetoDominance.LEFT;
import static org.opentripplanner.raptor.util.paretoset.ParetoDominance.MUTUAL;
import static org.opentripplanner.raptor.util.paretoset.ParetoDominance.NONE;
import static org.opentripplanner.raptor.util.paretoset.ParetoDominance.RIGHT;

import org.junit.jupiter.api.Test;

class ParetoDominanceTest {

  @Test
  void testOf() {
    assertEquals(MUTUAL, ParetoDominance.of("mutual"));
    assertEquals(LEFT, ParetoDominance.of("left"));
    assertEquals(RIGHT, ParetoDominance.of("right"));
    assertEquals(NONE, ParetoDominance.of("none"));
    assertEquals(LEFT, ParetoDominance.of("≺"));
    assertEquals(RIGHT, ParetoDominance.of("≻"));
    assertEquals(NONE, ParetoDominance.of("≡"));
    assertEquals(MUTUAL, ParetoDominance.of("∥"));

    // Mixed case
    assertEquals(LEFT, ParetoDominance.of("LEFT"));
    assertEquals(LEFT, ParetoDominance.of("lEfT"));
  }

  @Test
  void testOfLeftRight() {
    assertEquals(LEFT, ParetoDominance.of(true, false));
    assertEquals(RIGHT, ParetoDominance.of(false, true));
    assertEquals(MUTUAL, ParetoDominance.of(true, true));
    assertEquals(NONE, ParetoDominance.of(false, false));
  }

  @Test
  void symbol() {
    assertEquals('≺', LEFT.symbol());
    assertEquals('≻', RIGHT.symbol());
    assertEquals('≡', NONE.symbol());
    assertEquals('∥', MUTUAL.symbol());
  }

  @Test
  void testToString() {
    assertEquals("LEFT ≺", LEFT.toString());
    assertEquals("RIGHT ≻", RIGHT.toString());
    assertEquals("NONE ≡", NONE.toString());
    assertEquals("MUTUAL ∥", MUTUAL.toString());
  }
}
