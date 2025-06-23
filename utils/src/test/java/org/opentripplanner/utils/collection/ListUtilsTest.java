package org.opentripplanner.utils.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.utils.collection.ListUtils.first;
import static org.opentripplanner.utils.collection.ListUtils.last;
import static org.opentripplanner.utils.collection.ListUtils.requireAtLeastNElements;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ListUtilsTest {

  @Test
  void testFirst() {
    assertNull(first(null));
    assertNull(first(List.of()));
    assertEquals("A", first(List.of("A")));
    assertEquals("B", first(List.of("B", "C")));
  }

  @Test
  void testLast() {
    assertNull(last(null));
    assertNull(last(List.of()));
    assertEquals("A", last(List.of("A")));
    assertEquals("C", last(List.of("B", "C")));
  }

  @Test
  void combine() {
    var combined = ListUtils.combine(List.of(1, 2, 3), List.of(5, 6, 7));
    assertEquals(List.of(1, 2, 3, 5, 6, 7), combined);
  }

  private static String makeHello() {
    return new String("HELLO");
  }

  @Test
  void distinctByKey() {
    var first = new Wrapper(10, makeHello());
    var second = new Wrapper(20, makeHello());
    var third = new Wrapper(20, "HI");

    // the first and second elements have equal values for the "string" property but different
    // values for the "i" property
    var duplicates = List.of(first, second, third);

    // with a normal stream().distinct() or Set.copyOf no item would be removed but when
    // we tell the deduplication logic to only look at a single property those with equal
    // "string" values are de-duplicated
    var deduplicated = ListUtils.distinctByKey(duplicates, w -> w.string);

    // as a result the second element is removed from the result as its "string" value is
    // equal to the first element's
    assertEquals(List.of(first, third), deduplicated);
  }

  @Test
  void ofNullable() {
    assertEquals(List.of(), ListUtils.ofNullable(null));
    assertEquals(List.of("A"), ListUtils.ofNullable("A"));
  }

  @Test
  void testRequireAtLeastNElements() {
    var zeroElements = List.of();
    var oneElement = List.of("w");
    requireAtLeastNElements(zeroElements, 0);
    requireAtLeastNElements(oneElement, 1);

    assertThrows(NullPointerException.class, () -> requireAtLeastNElements(null, 0));
    assertThrows(IllegalArgumentException.class, () -> requireAtLeastNElements(zeroElements, 1));
    assertThrows(IllegalArgumentException.class, () -> requireAtLeastNElements(oneElement, 2));
  }

  private record Wrapper(int i, String string) {}

  @Nested
  class OverlappingPairs {

    @Test
    void overlappingPairs() {
      var list = List.of(1, 2, 3, 4);
      var res = ListUtils.partitionIntoOverlappingPairs(list);
      assertEquals(List.of(new Pair(1, 2), new Pair(2, 3), new Pair(3, 4)), res);
    }

    @Test
    void singleOverlappingPair() {
      var list = List.of(1, 2);
      var res = ListUtils.partitionIntoOverlappingPairs(list);
      assertEquals(List.of(new Pair(1, 2)), res);
    }

    @Test
    void singleItemInList() {
      var list = List.of(2);
      var res = ListUtils.partitionIntoOverlappingPairs(list);
      assertEquals(List.of(), res);
    }

    @Test
    void emptyList() {
      var list = List.of();
      var res = ListUtils.partitionIntoOverlappingPairs(list);
      assertEquals(List.of(), res);
    }
  }

  @Test
  void split() {
    var list = List.of(1, 2, 3, 4);
    var res = ListUtils.partitionIntoSplits(list);
    assertEquals(
      List.of(
        new Split<>(1, List.of(2, 3, 4)),
        new Split<>(2, List.of(3, 4)),
        new Split<>(3, List.of(4))
      ),
      res
    );
  }
}
