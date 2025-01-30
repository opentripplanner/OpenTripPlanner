package org.opentripplanner.utils.text;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.utils.text.TextVariablesSubstitution.insertVariables;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TextVariablesSubstitutionTest {

  @Test
  void testInsertVariablesInProperties() {
    Map<String, String> map = Map.ofEntries(
      entry("a", "A"),
      entry("b", "B"),
      entry("ab", "${a}${b}"),
      entry("ab2", "${ab} - ${a} - ${b}")
    );

    var result = insertVariables(map, this::errorHandler);

    assertEquals("A", result.get("a"));
    assertEquals("B", result.get("b"));
    assertEquals("AB", result.get("ab"));
    assertEquals("AB - A - B", result.get("ab2"));
  }

  @Test
  void testInsertVariablesInValue() {
    var map = Map.ofEntries(
      entry("a", "A"),
      entry("b", "B"),
      entry("ab", "${a}${b}"),
      entry("ab2", "${ab} - ${a} - ${b}")
    );

    assertEquals(
      "No substitution",
      insertVariables("No substitution", map::get, this::errorHandler)
    );
    assertEquals("A B", insertVariables("${a} ${b}", map::get, this::errorHandler));
    assertEquals("AB", insertVariables("${ab}", map::get, this::errorHandler));
    assertEquals("AB - A - B", insertVariables("${ab2}", map::get, this::errorHandler));
    var ex = assertThrows(
      IllegalArgumentException.class,
      () -> insertVariables("${c}", map::get, this::errorHandler)
    );
    assertEquals("c", ex.getMessage());
  }

  private void errorHandler(String name) {
    throw new IllegalArgumentException(name);
  }
}
