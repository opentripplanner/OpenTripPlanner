package org.opentripplanner.utils.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CollectionUtilsTest {

  public static final String NULL_STRING = "<null>";

  @Test
  void testIsEmpty() {
    assertTrue(CollectionUtils.isEmpty((List) null));
    assertTrue(CollectionUtils.isEmpty(List.of()));
    assertFalse(CollectionUtils.isEmpty(List.of(1)));
  }

  @Test
  void testHasValue() {
    assertFalse(CollectionUtils.hasValue(null));
    assertFalse(CollectionUtils.hasValue(List.of()));
    assertTrue(CollectionUtils.hasValue(List.of(1)));
  }

  @Test
  void testToString() {
    assertEquals("<null>", CollectionUtils.toString(null, NULL_STRING));
    assertEquals("[]", CollectionUtils.toString(Set.of(), NULL_STRING));
    assertEquals("[]", CollectionUtils.toString(List.of(), NULL_STRING));
    assertEquals("[JUNE]", CollectionUtils.toString(EnumSet.of(Month.JUNE), NULL_STRING));
    assertEquals(
      "[APRIL, JANUARY, JUNE]",
      CollectionUtils.toString(EnumSet.of(Month.JANUARY, Month.JUNE, Month.APRIL), "#")
    );

    // Given a list of objects
    List<Object> list = new ArrayList<>();
    list.add(Duration.ofHours(3));
    list.add(null);
    list.add(Month.JUNE);
    list.add(Month.APRIL);

    // Then: keep list order, do not sort
    assertEquals("[PT3H, <null>, JUNE, APRIL]", CollectionUtils.toString(list, NULL_STRING));

    // And: Set should be sorted alphabetically
    Set<Object> set = new HashSet<>(list);
    assertEquals("[<null>, APRIL, JUNE, PT3H]", CollectionUtils.toString(set, NULL_STRING));
  }

  @Test
  void testIsEmptyMap() {
    assertTrue(CollectionUtils.isEmpty((Map<Object, Object>) null));
    assertTrue(CollectionUtils.isEmpty(Map.of()));
    assertFalse(CollectionUtils.isEmpty(Map.of(1, 1)));
  }

  @Test
  void testIsEmptyCollection() {
    assertTrue(CollectionUtils.isEmpty((Collection<Object>) null));
    assertTrue(CollectionUtils.isEmpty(List.of()));
    assertFalse(CollectionUtils.isEmpty(Set.of(1)));
  }

  @Test
  void testRequireNullOrNonEmpty() {
    CollectionUtils.requireNullOrNonEmpty(null, "test");
    CollectionUtils.requireNullOrNonEmpty(List.of(1), "test");

    var ex = assertThrows(IllegalArgumentException.class, () -> {
      CollectionUtils.requireNullOrNonEmpty(List.of(), "test");
    });
    assertEquals("'test' must not be empty.", ex.getMessage());
  }
}
