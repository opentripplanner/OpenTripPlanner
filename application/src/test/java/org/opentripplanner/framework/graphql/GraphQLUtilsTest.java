package org.opentripplanner.framework.graphql;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import graphql.ExecutionInput;
import graphql.execution.ExecutionContext;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.support.graphql.DataFetchingSupport;
import org.opentripplanner.core.model.i18n.TranslatedString;

class GraphQLUtilsTest {

  static final ExecutionContext EXECUTION_CONTEXT;

  static {
    ExecutionInput executionInput = ExecutionInput.newExecutionInput()
      .query("")
      .locale(Locale.ENGLISH)
      .build();

    EXECUTION_CONTEXT = DataFetchingSupport.executionContext(executionInput);
  }

  @Test
  void testGetTranslationWithNullString() {
    var env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(EXECUTION_CONTEXT)
      .locale(Locale.ENGLISH)
      .build();

    var translation = GraphQLUtils.getTranslation(null, env);

    assertNull(translation);
  }

  @Test
  void testGetTranslationWithTranslations() {
    var env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(EXECUTION_CONTEXT)
      .locale(Locale.FRENCH)
      .build();

    var frenchTranslation = "translationFr";
    Map<String, String> translations = Map.ofEntries(
      entry("en", "translationEn"),
      entry("fr", frenchTranslation)
    );
    var translatedString = TranslatedString.getDeduplicatedI18NString(translations, false);

    var translation = GraphQLUtils.getTranslation(translatedString, env);

    assertEquals(frenchTranslation, translation);
  }

  @Test
  void testGetLocaleWithDefinedLocaleArg() {
    var env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(EXECUTION_CONTEXT)
      .localContext(Map.of("locale", Locale.GERMAN))
      .locale(Locale.ENGLISH)
      .build();

    var frenchLocale = Locale.FRENCH;

    var localeWithString = GraphQLUtils.getLocale(env, frenchLocale.toString());
    assertEquals(frenchLocale, localeWithString);

    var localeWithLocale = GraphQLUtils.getLocale(env, frenchLocale);
    assertEquals(frenchLocale, localeWithLocale);
  }

  @Test
  void testGetLocaleWithEnvLocale() {
    var frenchLocale = Locale.FRENCH;
    var env = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(EXECUTION_CONTEXT)
      .locale(Locale.FRENCH)
      .build();

    var locale = GraphQLUtils.getLocale(env);

    assertEquals(frenchLocale, locale);
  }

  @Test
  void testGetLocaleWithLocalContextLocale() {
    // Should use locale from local context even if env locale is defined

    var frenchLocale = Locale.FRENCH;
    var envWithNoLocale = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(EXECUTION_CONTEXT)
      .locale(Locale.GERMAN)
      .localContext(Map.of("locale", Locale.FRENCH))
      .build();

    var locale = GraphQLUtils.getLocale(envWithNoLocale);

    assertEquals(frenchLocale, locale);

    // Wildcard locale from env should not override locale from local context if it's defined

    var wildcardLocale = new Locale("*");

    var envWithWildcardLocale = DataFetchingEnvironmentImpl.newDataFetchingEnvironment(
      EXECUTION_CONTEXT
    )
      .locale(wildcardLocale)
      .localContext(Map.of("locale", Locale.FRENCH))
      .build();

    locale = GraphQLUtils.getLocale(envWithWildcardLocale);

    assertEquals(frenchLocale, locale);
  }
}
