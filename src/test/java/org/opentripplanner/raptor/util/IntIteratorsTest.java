package org.opentripplanner.raptor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor.util.IntIterators.intDecIterator;
import static org.opentripplanner.raptor.util.IntIterators.intIncIterator;
import static org.opentripplanner.raptor.util.IntIterators.singleValueIterator;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.spi.IntIterator;

public class IntIteratorsTest {

  @Test
  public void testIncrementByOne() {
    // Iterate from zero to zero is nothing
    assertEquals("[]", toString(intIncIterator(0, 0)));
    // One value
    assertEquals("[0]", toString(intIncIterator(0, 1)));

    // A few values starting from: 0
    assertEquals("[0, 1]", toString(intIncIterator(0, 2)));
    assertEquals("[0, 1, 2, 3]", toString(intIncIterator(0, 4)));

    // Start at value 1
    assertEquals("[1, 2, 3]", toString(intIncIterator(1, 4)));

    // From 5 to 5, return empty iterator
    assertEquals("[]", toString(intIncIterator(5, 5)));

    // Test some negative values
    assertEquals("[]", toString(intIncIterator(0, -9)));
    assertEquals("[-5, -4]", toString(intIncIterator(-5, -3)));
    assertEquals("[-2, -1, 0]", toString(intIncIterator(-2, 1)));
  }

  @Test
  public void testDecrementByOne() {
    // Iterate from N to N, gives an empty iterator
    assertEquals("[]", toString(intDecIterator(0, 0)));
    assertEquals("[]", toString(intDecIterator(5, 5)));

    // One value
    assertEquals("[1]", toString(intDecIterator(2, 1)));

    // A few values ends at 0 (exclusive)
    assertEquals("[2, 1]", toString(intDecIterator(3, 1)));
    assertEquals("[4, 3, 2, 1]", toString(intDecIterator(5, 1)));

    // A few values ends at 1 (exclusive)
    assertEquals("[5, 4, 3, 2]", toString(intDecIterator(6, 2)));

    // Test some negative values
    assertEquals("[]", toString(intDecIterator(-9, 0)));
    assertEquals("[-3, -4]", toString(intDecIterator(-2, -4)));
    assertEquals("[1, 0, -1]", toString(intDecIterator(2, -1)));
  }

  @Test
  public void testDecrementBy7() {
    // Iterate from N to N, gives an empty iterator
    assertEquals("[]", toString(intDecIterator(5, 5, 7)));

    // One value
    assertEquals("[7]", toString(intDecIterator(14, 1, 7)));
    assertEquals("[7]", toString(intDecIterator(14, 7, 7)));

    // A few values ends at 0 (inclusive)
    assertEquals("[9, 2]", toString(intDecIterator(16, 0, 7)));
    assertEquals("[14, 7, 0]", toString(intDecIterator(21, 0, 7)));

    // A few values ends at 1 (inclusive)
    assertEquals("[15, 8, 1]", toString(intDecIterator(22, 1, 7)));

    // Test some negative values
    assertEquals("[]", toString(intDecIterator(-9, 0, 7)));
    assertEquals("[-10]", toString(intDecIterator(-3, -11, 7)));
    assertEquals("[5, -2, -9]", toString(intDecIterator(12, -9, 7)));
  }

  @Test
  public void testSingleValueIterator() {
    assertEquals("[-3]", toString(singleValueIterator(-3)));
    assertEquals("[0]", toString(singleValueIterator(0)));
    assertEquals("[3]", toString(singleValueIterator(3)));
  }

  @Test
  public void testEmptyIterator() {
    assertEquals("[]", toString(IntIterators.empty()));
  }

  private static String toString(IntIterator it) {
    StringBuilder buf = new StringBuilder();
    boolean empty = true;

    while (it.hasNext()) {
      empty = false;
      buf.append(", ").append(it.next());
    }
    return empty ? "[]" : "[" + buf.substring(2) + "]";
  }
}
