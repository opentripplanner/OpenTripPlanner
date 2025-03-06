package org.opentripplanner.apis.transmodel.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OneOfInputValidatorTest {

  @Test
  void testValidateOneReturnsTheFieldName() {
    assertEquals(
      "two",
      OneOfInputValidator.validateOneOf(Map.of("two", "X"), "parent", "one", "two")
    );
  }

  @Test
  void testValidateOneOfWithEmptySetOfArguments() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      OneOfInputValidator.validateOneOf(Map.of(), "parent", "one", "two")
    );
    assertEquals(
      "No entries in 'parent @oneOf'. One of 'one', 'two' must be set.",
      ex.getMessage()
    );
  }

  @Test
  void testValidateOneOfWithTooManyArguments() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      OneOfInputValidator.validateOneOf(Map.of("one", "X", "two", "Y"), "parent", "one", "two")
    );
    assertEquals(
      "Only one entry in 'parent @oneOf' is allowed. Set: 'one', 'two'",
      ex.getMessage()
    );
  }

  @Test
  void testValidateOneOfWithEmptyCollection() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      OneOfInputValidator.validateOneOf(Map.of("one", List.of()), "parent", "one", "two")
    );
    assertEquals("'one' can not be empty in 'parent @oneOf'.", ex.getMessage());
  }
}
