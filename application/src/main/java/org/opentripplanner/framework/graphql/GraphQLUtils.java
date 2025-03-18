package org.opentripplanner.framework.graphql;

import graphql.schema.DataFetchingEnvironment;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;

public class GraphQLUtils {

  public static String getTranslation(
    @Nullable I18NString input,
    DataFetchingEnvironment environment
  ) {
    if (input == null) {
      return null;
    }
    return input.toString(getLocale(environment));
  }

  public static Locale getLocale(DataFetchingEnvironment environment) {
    var localeString = environment.getArgument("language");
    if (localeString != null) {
      return Locale.forLanguageTag((String) localeString);
    }

    return getLocaleFromEnvironment(environment);
  }

  public static Locale getLocale(
    DataFetchingEnvironment environment,
    @Nullable String localeString
  ) {
    if (localeString != null) {
      return Locale.forLanguageTag(localeString);
    }

    return getLocaleFromEnvironment(environment);
  }

  public static Locale getLocale(DataFetchingEnvironment environment, @Nullable Locale locale) {
    if (locale != null) {
      return locale;
    }

    return getLocaleFromEnvironment(environment);
  }

  public static Locale getLocaleFromEnvironment(DataFetchingEnvironment environment) {
    // This can come from the accept-language header
    var envLocale = environment.getLocale();
    // This can come from a locale param
    var localContextLocale = getDefaultLocale(environment);
    return localContextLocale.orElse(envLocale);
  }

  private static Optional<Locale> getDefaultLocale(DataFetchingEnvironment environment) {
    Map<String, ?> localContext = environment.getLocalContext();
    if (localContext == null) {
      return Optional.empty();
    }
    return Optional.ofNullable((Locale) localContext.get("locale"));
  }
}
