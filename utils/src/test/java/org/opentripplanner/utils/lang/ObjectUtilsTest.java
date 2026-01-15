package org.opentripplanner.utils.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ObjectUtilsTest {

  @Test
  void ifNotNull() {
    assertEquals("exp", ObjectUtils.ifNotNull("exp", "default"));
    assertEquals("default", ObjectUtils.ifNotNull(null, "default"));
    assertNull(ObjectUtils.ifNotNull(null, null));
  }

  @Test
  void ifNotNullFunctional() {
    var i = new IntBox(5);
    assertEquals(5, ObjectUtils.ifNotNull(i, IntBox::get, 7));
    assertEquals(7, ObjectUtils.ifNotNull(null, IntBox::get, 7));

    // Default is null
    assertEquals(5, ObjectUtils.ifNotNull(i, IntBox::get, null));
    assertNull(ObjectUtils.ifNotNull(null, IntBox::get, null));

    var o = new AtomicReference<String>(null);
    assertEquals("DEFAULT", ObjectUtils.ifNotNull(o, AtomicReference::get, "DEFAULT"));
    assertNull(ObjectUtils.ifNotNull(o, AtomicReference::get, null));
  }

  @Test
  void requireNotInitialized() {
    assertEquals("new", ObjectUtils.requireNotInitialized(null, "new"));
    var ex = assertThrows(IllegalStateException.class, () ->
      ObjectUtils.requireNotInitialized("foo", "old", "new")
    );
    assertEquals("Field foo is already set! Old value: old, new value: new.", ex.getMessage());

    ex = assertThrows(IllegalStateException.class, () ->
      ObjectUtils.requireNotInitialized("old", "new")
    );
    assertEquals("Field is already set! Old value: old, new value: new.", ex.getMessage());
  }

  @Test
  void safeGetOrNull() {
    assertEquals("test", ObjectUtils.safeGetOrNull(() -> "test"));
    assertEquals(3000, ObjectUtils.safeGetOrNull(() -> Duration.ofSeconds(3).toMillis()));
    assertNull(ObjectUtils.safeGetOrNull(() -> null));
    assertNull(
      ObjectUtils.safeGetOrNull(() -> {
        throw new NullPointerException("Something went wrong - ignore");
      })
    );
  }

  @Test
  void toStringTest() {
    assertEquals("1", ObjectUtils.toString(1));
    assertEquals("", ObjectUtils.toString(null));
  }

  private static Stream<Arguments> oneOfCases() {
    Object object = new Object();
    return Stream.of(
      Arguments.argumentSet("both are null", null, null, false),
      Arguments.argumentSet("only a is non-null", object, null, true),
      Arguments.argumentSet("only b is non-null", null, object, true),
      Arguments.argumentSet("both are non-null", object, object, false)
    );
  }

  @ParameterizedTest
  @MethodSource("oneOfCases")
  void oneOf(Object a, Object b, boolean expected) {
    assertEquals(expected, ObjectUtils.oneOf(a, b));
  }
}
