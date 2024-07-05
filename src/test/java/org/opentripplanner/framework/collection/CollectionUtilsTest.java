package org.opentripplanner.framework.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.type.Month;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CollectionUtilsTest {

  public static final String NULL_STRING = "<null>";

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
}
