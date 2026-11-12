package org.opentripplanner.raptor.util.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class IntIndexListTest {

  @Test
  void emptyInput() {
    var subject = IntIndexList.<Integer>of(List.of(), i -> i);

    assertEquals(-1, subject.maxIndex());
    assertEquals(List.of(), subject.get(0));
    assertEquals(List.of(), subject.stream().toList());
    assertSame(IntIndexList.empty(), subject);
  }

  @Test
  void groupsElementsByIndexAndKeepsOrder() {
    var subject = IntIndexList.of(List.of(0, 1, 2, 3, 4, 5), i -> i % 2);

    assertEquals(1, subject.maxIndex());
    assertEquals(List.of(0, 2, 4), subject.get(0));
    assertEquals(List.of(1, 3, 5), subject.get(1));
  }

  @Test
  void sizeIsHighestIndexPlusOneAndGapsAreEmptyGroups() {
    var subject = IntIndexList.of(List.of("a", "b"), s -> s.equals("a") ? 0 : 2);

    assertEquals(2, subject.maxIndex());
    assertEquals(List.of("a"), subject.get(0));
    assertEquals(List.of(), subject.get(1));
    assertEquals(List.of("b"), subject.get(2));
  }

  @Test
  void indexesOutOfRangeGiveEmptyList() {
    var subject = IntIndexList.of(List.of(1), i -> i);

    assertEquals(List.of(), subject.get(-1));
    assertEquals(List.of(), subject.get(2));
    assertEquals(List.of(), subject.get(1000));
  }

  @Test
  void inputInAnyOrder() {
    var subject = IntIndexList.of(List.of(2, 0, 1, 2), i -> i);

    assertEquals(2, subject.maxIndex());
    assertEquals(List.of(0), subject.get(0));
    assertEquals(List.of(1), subject.get(1));
    assertEquals(List.of(2, 2), subject.get(2));
  }

  @Test
  void streamCoversAllGroupsInIndexOrder() {
    var subject = IntIndexList.of(List.of(2, 0, 1, 2), i -> i);

    assertEquals(List.of(0, 1, 2, 2), subject.stream().toList());
  }

  @Test
  void elementsIsALazyFlatViewInIndexOrder() {
    var subject = IntIndexList.of(List.of(3, 0, 3, 1), i -> i);

    var result = new ArrayList<Integer>();
    subject.elements().forEach(result::add);
    assertEquals(List.of(0, 1, 3, 3), result);

    // The view can be iterated more than once, and an empty list has no elements
    var second = new ArrayList<Integer>();
    subject.elements().forEach(second::add);
    assertEquals(result, second);
    assertFalse(IntIndexList.<Integer>empty().elements().iterator().hasNext());
    assertThrows(NoSuchElementException.class, () ->
      IntIndexList.<Integer>empty().elements().iterator().next()
    );
  }

  @Test
  void filterKeepsIndexesAndTrimsTrailingEmptyGroups() {
    var subject = IntIndexList.of(List.of(0, 1, 2, 2, 3), i -> i);

    var result = subject.filter(i -> i == 1 || i == 2);
    assertEquals(2, result.maxIndex());
    assertEquals(List.of(), result.get(0));
    assertEquals(List.of(1), result.get(1));
    assertEquals(List.of(2, 2), result.get(2));

    assertEquals(1, subject.filter(i -> i < 2).maxIndex());
    assertEquals(-1, subject.filter(i -> false).maxIndex());
  }

  @Test
  void ofCanFilterWhileGrouping() {
    var subject = IntIndexList.of(List.of(0, 1, 2, 3, 4), i -> i % 2 == 0, i -> i / 2);

    assertEquals(2, subject.maxIndex());
    assertEquals(List.of(0), subject.get(0));
    assertEquals(List.of(2), subject.get(1));
    assertEquals(List.of(4), subject.get(2));
  }

  @Test
  void isImmutable() {
    var input = new ArrayList<>(List.of(0, 1));
    var subject = IntIndexList.of(input, i -> i);
    input.add(1);

    assertEquals(List.of(1), subject.get(1));
    assertThrows(UnsupportedOperationException.class, () -> subject.get(1).add(5));
  }
}
