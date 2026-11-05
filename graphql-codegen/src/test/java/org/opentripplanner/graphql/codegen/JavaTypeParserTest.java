package org.opentripplanner.graphql.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.palantir.javapoet.ClassName;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JavaTypeParserTest {

  private final JavaTypeParser parser = new JavaTypeParser(
    Map.of(
      "Edge",
      ClassName.get("graphql.relay", "Edge"),
      "TransitAlert",
      ClassName.get("org.example", "TransitAlert"),
      "Map",
      ClassName.get("java.util", "Map")
    )
  );

  @Test
  void importedGeneric() {
    assertEquals(
      "graphql.relay.Edge<org.example.TransitAlert>",
      parser.parse("Edge<TransitAlert>").toString()
    );
  }

  @Test
  void nestedTypeWithMultipleTypeArguments() {
    assertEquals(
      "java.util.Map.Entry<java.lang.String, java.lang.String>",
      parser.parse("Map.Entry<String, String>").toString()
    );
  }

  @Test
  void fullyQualifiedName() {
    assertEquals(
      "java.util.List<org.foo.Bar.Inner>",
      parser.parse("java.util.List<org.foo.Bar.Inner>").toString()
    );
  }

  @Test
  void javaLangType() {
    assertEquals("java.lang.Object", parser.parse("Object").toString());
  }

  @Test
  void unresolvableName() {
    assertThrows(IllegalArgumentException.class, () -> parser.parse("Unknown"));
  }

  @Test
  void malformedExpression() {
    assertThrows(IllegalArgumentException.class, () -> parser.parse("Edge<TransitAlert"));
    assertThrows(IllegalArgumentException.class, () -> parser.parse("Edge<TransitAlert>>"));
  }
}
