package org.opentripplanner.framework.graphql;

import graphql.schema.DataFetchingEnvironment;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;

/**
 * This class should always be used when translating fields or handling locales in GraphQL queries.
 */
public class GraphQLUtils {

  /**
   * If input is {@code null}, return null. Otherwise, input is translated using a locale from this
   * prioritized list:
   * <ol>
   *   <li>
   *   {@code language} parameter of a queried field.
   *   </li>
   *   <li>
   *   Locale from the DataFetchingEnvironment's local context (for journey planning queries this is {@code locale} parameter).
   *   </li>
   *   <li>
   *   DataFetchingEnvironment's locale which comes from the accept-language header.
   *   </li>
   *   <li>
   *   Default locale.
   *   </li>
   * </ol>
   */
  @Nullable
  public static String getTranslation(
    @Nullable I18NString input,
    DataFetchingEnvironment environment
  ) {
    if (input == null) {
      return null;
    }
    return input.toString(getLocale(environment));
  }

  /**
   * Returns locale from this prioritized list:
   * <ol>
   *   <li>
   *   {@code language} parameter of a queried field.
   *   </li>
   *   <li>
   *   Locale from the DataFetchingEnvironment's local context (for journey planning queries this is {@code locale} parameter).
   *   </li>
   *   <li>
   *   DataFetchingEnvironment's locale which comes from the accept-language header.
   *   </li>
   *   <li>
   *   Default locale.
   *   </li>
   * </ol>
   */
  public static Locale getLocale(DataFetchingEnvironment environment) {
    var localeString = environment.getArgument("language");
    if (localeString != null) {
      return Locale.forLanguageTag((String) localeString);
    }

    return getLocaleFromEnvironment(environment);
  }

  /**
   * Returns locale from this prioritized list:
   * <ol>
   *   <li>
   *   {@code localeString}.
   *   </li>
   *   <li>
   *   Locale from the DataFetchingEnvironment's local context (for journey planning queries this is {@code locale} parameter).
   *   </li>
   *   <li>
   *   DataFetchingEnvironment's locale which comes from the accept-language header.
   *   </li>
   *   <li>
   *   Default locale.
   *   </li>
   * </ol>
   */
  public static Locale getLocale(
    DataFetchingEnvironment environment,
    @Nullable String localeString
  ) {
    if (localeString != null) {
      return Locale.forLanguageTag(localeString);
    }

    return getLocaleFromEnvironment(environment);
  }

  /**
   * Returns locale from this prioritized list:
   * <ol>
   *   <li>
   *   {@code locale}.
   *   </li>
   *   <li>
   *   Locale from the DataFetchingEnvironment's local context (for journey planning queries this is {@code locale} parameter).
   *   </li>
   *   <li>
   *   DataFetchingEnvironment's locale which comes from the accept-language header.
   *   </li>
   *   <li>
   *   Default locale.
   *   </li>
   * </ol>
   */
  public static Locale getLocale(DataFetchingEnvironment environment, @Nullable Locale locale) {
    if (locale != null) {
      return locale;
    }

    return getLocaleFromEnvironment(environment);
  }

  /**
   * Returns locale from this prioritized list:
   * <ol>
   *   <li>
   *   Locale from the DataFetchingEnvironment's local context (for journey planning queries this is {@code locale} parameter).
   *   </li>
   *   <li>
   *   DataFetchingEnvironment's locale which comes from the accept-language header.
   *   </li>
   *   <li>
   *   Default locale.
   *   </li>
   * </ol>
   */
  public static Locale getLocaleFromEnvironment(DataFetchingEnvironment environment) {
    // This can come from the accept-language header
    var envLocale = environment.getLocale();
    // This can come from a locale param
    var localContextLocale = getLocalContextLocale(environment);
    return localContextLocale.orElse(envLocale);
  }

  /**
   * Returns locale from the DataFetchingEnvironment's local context (for journey planning queries
   * this is {@code locale} parameter) or empty if none exist.
   */
  private static Optional<Locale> getLocalContextLocale(DataFetchingEnvironment environment) {
    Map<String, ?> localContext = environment.getLocalContext();
    if (localContext == null) {
      return Optional.empty();
    }
    return Optional.ofNullable((Locale) localContext.get("locale"));
  }
}
