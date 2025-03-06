package org.opentripplanner.apis.transmodel.support;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import graphql.ExecutionInput;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class GqlUtilTest {

  static final ExecutionContext executionContext;
  private static final String TEST_ARGUMENT = "testArgument";

  static {
    ExecutionInput executionInput = ExecutionInput.newExecutionInput()
      .query("")
      .locale(Locale.ENGLISH)
      .build();

    executionContext = newExecutionContextBuilder()
      .executionInput(executionInput)
      .executionId(ExecutionId.from("GqlUtilTest"))
      .build();
  }

  @Test
  void testGetPositiveNonNullIntegerArgumentWithStrictlyPositiveValue() {
    var env = buildEnvWithTestValue(1);
    assertEquals(1, GqlUtil.getPositiveNonNullIntegerArgument(env, TEST_ARGUMENT));
  }

  @Test
  void testGetPositiveNonNullIntegerArgumentWithZeroValue() {
    var env = buildEnvWithTestValue(0);
    assertEquals(0, GqlUtil.getPositiveNonNullIntegerArgument(env, TEST_ARGUMENT));
  }

  @Test
  void testGetPositiveNonNullIntegerArgumentWithNegativeValue() {
    var env = buildEnvWithTestValue(-1);
    assertThrows(IllegalArgumentException.class, () ->
      GqlUtil.getPositiveNonNullIntegerArgument(env, TEST_ARGUMENT)
    );
  }

  @Test
  void testGetPositiveNonNullIntegerArgumentWithNullValue() {
    var env = buildEnvWithTestValue(null);
    assertThrows(IllegalArgumentException.class, () ->
      GqlUtil.getPositiveNonNullIntegerArgument(env, TEST_ARGUMENT)
    );
  }

  @Test
  void testGetPositiveNonNullIntegerArgumentWithoutValue() {
    var env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext).build();
    assertThrows(IllegalArgumentException.class, () ->
      GqlUtil.getPositiveNonNullIntegerArgument(env, TEST_ARGUMENT)
    );
  }

  private static DataFetchingEnvironment buildEnvWithTestValue(Integer value) {
    Map<String, Object> argsMap = new HashMap<>();
    argsMap.put(TEST_ARGUMENT, value);
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .arguments(argsMap)
      .build();
  }

  @Test
  void testGetLocaleWithLangArgument() {
    var env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .locale(Locale.ENGLISH)
      .arguments(Map.of("lang", "fr"))
      .build();

    var locale = GqlUtil.getLocale(env);

    assertEquals(Locale.FRENCH, locale);
  }

  @Test
  void testGetLocaleWithLanguageArgument() {
    var env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .locale(Locale.ENGLISH)
      .arguments(Map.of("language", "fr"))
      .build();

    var locale = GqlUtil.getLocale(env);

    assertEquals(Locale.FRENCH, locale);
  }

  @Test
  void testGetLocaleWithBothArguments() {
    var env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .locale(Locale.ENGLISH)
      .arguments(Map.of("lang", "de", "language", "fr"))
      .build();

    var locale = GqlUtil.getLocale(env);

    assertEquals(Locale.GERMAN, locale);
  }

  @Test
  void testGetLocaleWithoutArguments() {
    var env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .locale(Locale.ENGLISH)
      .build();

    var locale = GqlUtil.getLocale(env);

    assertEquals(Locale.ENGLISH, locale);
  }
}
