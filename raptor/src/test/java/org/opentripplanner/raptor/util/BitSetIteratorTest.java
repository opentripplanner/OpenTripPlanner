package org.opentripplanner.raptor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;
import org.junit.jupiter.api.Test;

public class BitSetIteratorTest {

  @Test
  public void test() {
    BitSetIterator it;
    BitSet set = new BitSet(5);

    // Empty set does not have any elements
    it = new BitSetIterator(set);
    assertFalse(it.hasNext());

    set.set(2);
    it = new BitSetIterator(set);
    assertTrue(it.hasNext());
    assertEquals(2, it.next());
    assertFalse(it.hasNext());

    // set: [0, 2, 5], 2 is added above
    set.set(0);
    set.set(5);
    it = new BitSetIterator(set);
    assertTrue(it.hasNext());
    assertEquals(0, it.next());
    assertTrue(it.hasNext());
    assertEquals(2, it.next());
    assertTrue(it.hasNext());
    assertEquals(5, it.next());
    assertFalse(it.hasNext());
  }
}
