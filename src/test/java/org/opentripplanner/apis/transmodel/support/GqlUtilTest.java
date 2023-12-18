package org.opentripplanner.apis.transmodel.support;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import graphql.ExecutionInput;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class GqlUtilTest {

  static final ExecutionContext executionContext;

  static {
    ExecutionInput executionInput = ExecutionInput
      .newExecutionInput()
      .query("")
      .locale(Locale.ENGLISH)
      .build();

    executionContext =
      newExecutionContextBuilder()
        .executionInput(executionInput)
        .executionId(ExecutionId.from("GqlUtilTest"))
        .build();
  }

  @Test
  void testGetLocaleWithLangArgument() {
    var env = DataFetchingEnvironmentImpl
      .newDataFetchingEnvironment(executionContext)
      .locale(Locale.ENGLISH)
      .arguments(Map.of("lang", "fr"))
      .build();

    var locale = GqlUtil.getLocale(env);

    assertEquals(Locale.FRENCH, locale);
  }

  @Test
  void testGetLocaleWithLanguageArgument() {
    var env = DataFetchingEnvironmentImpl
      .newDataFetchingEnvironment(executionContext)
      .locale(Locale.ENGLISH)
      .arguments(Map.of("language", "fr"))
      .build();

    var locale = GqlUtil.getLocale(env);

    assertEquals(Locale.FRENCH, locale);
  }

  @Test
  void testGetLocaleWithBothArguments() {
    var env = DataFetchingEnvironmentImpl
      .newDataFetchingEnvironment(executionContext)
      .locale(Locale.ENGLISH)
      .arguments(Map.of("lang", "de", "language", "fr"))
      .build();

    var locale = GqlUtil.getLocale(env);

    assertEquals(Locale.GERMAN, locale);
  }

  @Test
  void testGetLocaleWithoutArguments() {
    var env = DataFetchingEnvironmentImpl
      .newDataFetchingEnvironment(executionContext)
      .locale(Locale.ENGLISH)
      .build();

    var locale = GqlUtil.getLocale(env);

    assertEquals(Locale.ENGLISH, locale);
  }
}
