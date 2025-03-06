package org.opentripplanner.transit.api.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class RequiredFilterValuesTest {

  @Test
  void testEmptyIsInvalid() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
      FilterValues.ofRequired("empty", List.of());
    });
    assertEquals("Filter empty values must not be empty.", e.getMessage());
  }

  @Test
  void testNullIsInvalid() {
    List<String> nullList = null;
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
      FilterValues.ofRequired("null", nullList);
    });
    assertEquals("Filter null values must not be empty.", e.getMessage());
  }
}
