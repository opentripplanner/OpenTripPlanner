package org.opentripplanner.framework.token;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.token.AdvancedTokenSchemaTest.TestCase.testCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AdvancedTokenSchemaTest implements TestTokenSchemaConstants {

  private static final List<TestCase> TEST_CASES = new ArrayList<>();

  static {
    // Version 1: [ENUM]
    var builder = TokenSchema.ofVersion(1).addEnum(ENUM_FIELD);
    TEST_CASES.add(
      TestCase.testCase(builder, "(v1, MAY)", it -> it.encode().withEnum(ENUM_FIELD, ENUM_VALUE))
    );

    // Version 2: [ENUM, DURATION, INT]
    builder = builder.newVersion().addDuration(DURATION_FIELD).addInt(INT_FIELD);
    TEST_CASES.add(
      testCase(
        builder,
        "(v2, MAY, PT2M13S, 31)",
        // We can add named fields in any order(token order: byte,Duration,Int)
        it ->
          it
            .encode()
            .withInt(INT_FIELD, INT_VALUE)
            .withEnum(ENUM_FIELD, ENUM_VALUE)
            .withDuration(DURATION_FIELD, DURATION_VALUE)
      )
    );

    // Version 3 - [ENUM, @deprecated DURATION, INT]
    builder = builder.newVersion().deprecate(DURATION_FIELD);
    TEST_CASES.add(
      testCase(builder, "(v3, MAY, 31)", it ->
        it
          .encode()
          .withInt(INT_FIELD, INT_VALUE)
          .withEnum(ENUM_FIELD, ENUM_VALUE)
          .withDuration(DURATION_FIELD, DURATION_VALUE)
      )
    );

    // Version 4 - [ENUM, INT, STRING]
    builder = builder.newVersion().addString(STRING_FIELD);
    TEST_CASES.add(
      testCase(builder, "(v4, MAY, 31, text)", it ->
        it
          .encode()
          .withInt(INT_FIELD, INT_VALUE)
          .withEnum(ENUM_FIELD, ENUM_VALUE)
          .withString(STRING_FIELD, STRING_VALUE)
      )
    );

    // Version 5 - [@deprecated ENUM, INT, STRING, TIME_INSTANT]
    builder = builder.newVersion().deprecate(ENUM_FIELD).addTimeInstant(TIME_INSTANT_FIELD);
    TEST_CASES.add(
      testCase(builder, "(v5, 31, text, 2023-10-23T10:00:59Z)", it ->
        it
          .encode()
          .withInt(INT_FIELD, INT_VALUE)
          .withEnum(ENUM_FIELD, ENUM_VALUE)
          .withTimeInstant(TIME_INSTANT_FIELD, TIME_INSTANT_VALUE)
          .withString(STRING_FIELD, STRING_VALUE)
      )
    );
    // Version 6 - [INT, STRING, TIME_INSTANT]
    builder = builder.newVersion();
    TEST_CASES.add(
      testCase(builder, "(v6, 31, text, 2023-10-23T10:00:59Z)", it ->
        it
          .encode()
          .withInt(INT_FIELD, INT_VALUE)
          .withTimeInstant(TIME_INSTANT_FIELD, TIME_INSTANT_VALUE)
          .withString(STRING_FIELD, STRING_VALUE)
      )
    );
  }

  private static List<TestCase> testCases() {
    return TEST_CASES;
  }

  @ParameterizedTest
  @MethodSource(value = "testCases")
  void testDecodeBackwardsCompatibility(TestCase testCase) {
    allTestCasesFrom(testCase).forEach(s ->
      assertEquals(testCase.expected(), s.decode(testCase.token()).toString())
    );
  }

  @ParameterizedTest
  @MethodSource(value = "testCases")
  void testDecodeForwardCompatibility(TestCase testCase) {
    nextTestCase(testCase)
      .map(TestCase::token)
      .ifPresent(nextVersionToken ->
        assertEquals(testCase.expected(), testCase.subject().decode(nextVersionToken).toString())
      );
  }

  @Test
  void testMerge() {
    var merged = TokenSchema.ofVersion(6)
      .addInt(INT_FIELD)
      .addString(STRING_FIELD)
      .addTimeInstant(TIME_INSTANT_FIELD)
      .build();

    var subjectV6 = TEST_CASES.get(5).subject();
    assertEquals(merged.currentDefinition(), subjectV6.currentDefinition());
  }

  @Test
  void testDefinitionToString() {
    var expected = List.of(
      "TokenDefinition{version: 1, fields: [EnField:ENUM]}",
      "TokenDefinition{version: 2, fields: [EnField:ENUM, ADur:DURATION, ANum:INT]}",
      "TokenDefinition{version: 3, fields: [EnField:ENUM, @deprecated ADur:DURATION, ANum:INT]}",
      "TokenDefinition{version: 4, fields: [EnField:ENUM, ANum:INT, AStr:STRING]}",
      "TokenDefinition{version: 5, fields: [@deprecated EnField:ENUM, ANum:INT, AStr:STRING, ATime:TIME_INSTANT]}",
      "TokenDefinition{version: 6, fields: [ANum:INT, AStr:STRING, ATime:TIME_INSTANT]}"
    );
    for (int i = 0; i < TEST_CASES.size(); i++) {
      assertEquals(expected.get(i), TEST_CASES.get(i).subject().currentDefinition().toString());
    }
  }

  /**
   * List of all test-cases including the given test-case until the end of all test-cases
   */
  private static Stream<TokenSchema> allTestCasesFrom(TestCase testCase) {
    return TEST_CASES.subList(TEST_CASES.indexOf(testCase), TEST_CASES.size() - 1)
      .stream()
      .map(TestCase::subject);
  }

  private static Optional<TestCase> nextTestCase(TestCase testCase) {
    int index = TEST_CASES.indexOf(testCase) + 1;
    return index < TEST_CASES.size() ? Optional.of(TEST_CASES.get(index)) : Optional.empty();
  }

  record TestCase(TokenSchema subject, String expected, String token) {
    static TestCase testCase(
      TokenDefinitionBuilder definitionBuilder,
      String expected,
      Function<TokenSchema, TokenBuilder> builder
    ) {
      var schema = definitionBuilder.build();
      return new TestCase(schema, expected, builder.apply(schema).build());
    }

    @Override
    public String toString() {
      return subject.currentDefinition().toString();
    }
  }
}
