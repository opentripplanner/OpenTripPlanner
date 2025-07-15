package org.opentripplanner.apis.support.graphql.injectdoc;

import static com.google.common.truth.Truth.assertThat;

import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.SchemaTransformer;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.SchemaPrinter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.text.TextAssertions;

/**
 * This test reads in a schema file, injects documentation and convert the
 * new schema to an SDL text string. The result is then compared to the
 * "expected" SDL file. The input and expected files are found in the
 * resources - with the same name as this test.
 */
class InjectCustomDocumentationTest {

  private GraphQLSchema schema;
  private String sdlExpected;

  @BeforeEach
  void setUp() throws IOException {
    var sdl = loadSchemaResource(".graphql");
    sdlExpected = loadSchemaResource(".graphql.expected");

    var parser = new SchemaParser();
    var generator = new SchemaGenerator();
    var typeRegistry = parser.parse(sdl);
    schema = generator.makeExecutableSchema(typeRegistry, buildRuntimeWiring());
  }

  private static RuntimeWiring buildRuntimeWiring() {
    return RuntimeWiring.newRuntimeWiring()
      .type("QueryType", b -> b.dataFetcher("listE", e -> List.of()))
      .type("En", b -> b.enumValues(n -> n))
      .type("AB", b -> b.typeResolver(it -> null))
      .type("AC", b -> b.typeResolver(it -> null))
      .scalar(
        GraphQLScalarType.newScalar()
          .name("Duration")
          .coercing(new Coercing<String, String>() {})
          .build()
      )
      .build();
  }

  /**
   * Return a map of documentation key/values. The
   * value is the same as the key for easy recognition.
   */
  static Map<String, String> text() {
    return Stream.of(
      "AB.description",
      "AC.description.append",
      "AType.description",
      "AType.a.description",
      "AType.b.deprecated",
      "BType.description",
      "BType.a.description",
      "BType.a.deprecated",
      "CType.description.append",
      "CType.a.description.append",
      "CType.b.deprecated.append",
      "QueryType.findAB.description",
      "QueryType.getAC.deprecated",
      "AEnum.description",
      "AEnum.E1.description",
      "AEnum.E2.deprecated",
      "AEnum.E3.deprecated",
      "Duration.description",
      "InputType.description",
      "InputType.a.description",
      "InputType.b.deprecated",
      "InputType.c.deprecated"
    ).collect(Collectors.toMap(e -> e, e -> e));
  }

  @Test
  void test() {
    Map<String, String> texts = text();
    var customDocumentation = new CustomDocumentation(texts);
    var visitor = new InjectCustomDocumentation(customDocumentation);
    var newSchema = SchemaTransformer.transformSchema(schema, visitor);
    var p = new SchemaPrinter();
    var result = p.print(newSchema);

    var missingValues = texts
      .values()
      .stream()
      .sorted()
      .filter(it -> !result.contains(it))
      .toList();

    assertThat(missingValues).isEmpty();

    TextAssertions.assertLinesEquals(sdlExpected, result);
  }

  @SuppressWarnings("DataFlowIssue")
  String loadSchemaResource(String suffix) throws IOException {
    var cl = getClass();
    var name = cl.getName().replace('.', '/') + suffix;
    return new String(
      ClassLoader.getSystemResourceAsStream(name).readAllBytes(),
      StandardCharsets.UTF_8
    );
  }
}
