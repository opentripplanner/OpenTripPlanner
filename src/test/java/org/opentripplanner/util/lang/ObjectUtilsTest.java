package org.opentripplanner.util.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ObjectUtilsTest {

  @Test
  void ifNotNull() {
    assertEquals("exp", ObjectUtils.ifNotNull("exp", "default"));
    assertEquals("default", ObjectUtils.ifNotNull(null, "default"));
    assertNull(ObjectUtils.ifNotNull(null, null));
  }
}
