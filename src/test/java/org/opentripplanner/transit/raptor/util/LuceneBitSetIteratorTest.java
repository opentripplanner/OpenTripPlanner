package org.opentripplanner.transit.raptor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.FixedBitSet;
import org.junit.jupiter.api.Test;

public class LuceneBitSetIteratorTest {

  @Test
  public void test() {
    LuceneBitSetIterator it;
    BitSet set = new FixedBitSet(6);

    // Empty set does not have any elements
    it = new LuceneBitSetIterator(set);
    assertFalse(it.hasNext());

    set.set(2);
    it = new LuceneBitSetIterator(set);
    assertTrue(it.hasNext());
    assertEquals(2, it.next());
    assertFalse(it.hasNext());

    // set: [0, 2, 5], 2 is added above
    set.set(0);
    set.set(5);
    it = new LuceneBitSetIterator(set);
    assertTrue(it.hasNext());
    assertEquals(0, it.next());
    assertTrue(it.hasNext());
    assertEquals(2, it.next());
    assertTrue(it.hasNext());
    assertEquals(5, it.next());
    assertFalse(it.hasNext());
  }
}
