package org.opentripplanner.transit.api.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class RequiredFilterValuesTest {

  @Test
  void testEmptyIsInvalid() {
    assertThrows(
      IllegalArgumentException.class,
      () -> {
        FilterValues.ofEmptyIsInvalid("empty", List.of());
      }
    );
  }

  @Test
  void testNullIsInvalid() {
    List<String> nullList = null;
    assertThrows(
      IllegalArgumentException.class,
      () -> {
        FilterValues.ofEmptyIsInvalid("null", nullList);
      }
    );
  }
}
