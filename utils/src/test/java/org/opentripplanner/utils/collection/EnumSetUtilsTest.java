package org.opentripplanner.utils.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.utils.collection.EnumSetUtilsTest.Foo.BAR;
import static org.opentripplanner.utils.collection.EnumSetUtilsTest.Foo.CODE;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EnumSetUtilsTest {

  @Test
  void unmodifiableEnumSet() {
    assertEquals(Set.of(), EnumSetUtils.unmodifiableEnumSet(List.of(), Foo.class));
    assertEquals(Set.of(BAR), EnumSetUtils.unmodifiableEnumSet(List.of(BAR), Foo.class));
    assertEquals(
      Set.of(BAR, CODE),
      EnumSetUtils.unmodifiableEnumSet(List.of(BAR, CODE), Foo.class)
    );
  }

  enum Foo {
    BAR,
    CODE,
  }
}
