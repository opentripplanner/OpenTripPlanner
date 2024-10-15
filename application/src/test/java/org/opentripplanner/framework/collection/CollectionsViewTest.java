package org.opentripplanner.framework.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

class CollectionsViewTest {

  @SuppressWarnings("ConstantConditions")
  @Test
  void test() {
    List<String> a = new ArrayList<>(List.of("A"));
    List<String> b = new ArrayList<>(List.of("B"));

    var subject = new CollectionsView<>(a, b);

    // Check iteration
    Iterator<String> it = subject.iterator();
    assertTrue(it.hasNext());

    // Make sure the hasNext() call do not change the state of the iterator
    assertTrue(it.hasNext());

    assertEquals("A", it.next());
    assertTrue(it.hasNext());
    assertEquals("B", it.next());
    assertFalse(it.hasNext());

    // When all collections are processed, we should get false - without any failure
    assertFalse(it.hasNext());

    // Check size
    assertEquals(2, subject.size());

    // Test internal iterator and toString() work as expected
    assertEquals("[A, B]", subject.toString());

    // Verify stream works
    assertEquals("AB", subject.stream().reduce("", (acc, e) -> acc + e));

    // Modify underlying lists
    a.addAll(List.of("A1", "A2"));
    b.remove("B");

    it = subject.iterator();
    assertTrue(it.hasNext());
    assertEquals("A", it.next());
    assertTrue(it.hasNext());
    assertEquals("A1", it.next());
    assertTrue(it.hasNext());
    assertEquals("A2", it.next());
    assertFalse(it.hasNext());

    // Check size
    assertEquals(3, subject.size());
  }
}
