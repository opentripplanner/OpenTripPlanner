package org.opentripplanner.utils.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoxTest {

  public static final String VALUE = "Test";

  @Test
  void getValue() {
    assertEquals(VALUE, Box.of(VALUE).get());
    assertNull(Box.empty().get());
  }

  @Test
  void setValue() {
    Box<String> box = Box.empty();
    box.set(VALUE);
    assertEquals(VALUE, box.get());
  }

  @Test
  void modifyValue() {
    Box<String> box = Box.of("A");
    box.modify(v -> v + "B");
    assertEquals("AB", box.get());
  }

  @Test
  void testIsEmpty() {
    assertTrue(Box.<String>empty().isEmpty());
    assertFalse(Box.of("A").isEmpty());
  }

  @Test
  void testEquals() {
    assertEquals(Box.of(1), Box.of(1));
    assertNotEquals(Box.of(1), Box.of(2));

    assertEquals(Box.of(1).hashCode(), Box.of(1).hashCode());
    assertNotEquals(Box.of(1).hashCode(), Box.of(2).hashCode());
  }

  @Test
  void testToString() {
    assertEquals("[Test]", Box.of(VALUE).toString());
  }
}
