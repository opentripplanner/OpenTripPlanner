package org.opentripplanner.framework.graphql;

import graphql.schema.DataFetchingEnvironment;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.I18NString;

public class GraphQLUtils {

  public static String getTranslation(I18NString input, DataFetchingEnvironment environment) {
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

  public static Locale getLocale(DataFetchingEnvironment environment, String localeString) {
    if (localeString != null) {
      return Locale.forLanguageTag(localeString);
    }

    return getLocaleFromEnvironment(environment);
  }

  public static Locale getLocale(DataFetchingEnvironment environment, Locale locale) {
    if (locale != null) {
      return locale;
    }

    return getLocaleFromEnvironment(environment);
  }

  public static Locale getLocaleFromEnvironment(DataFetchingEnvironment environment) {
    // This can come from the accept-language header
    var userLocale = environment.getLocale();
    var defaultLocale = getDefaultLocale(environment);

    if (userLocale == null) {
      return defaultLocale.orElse(Locale.forLanguageTag("*"));
    }

    if (defaultLocale.isPresent() && acceptAnyLocale(userLocale)) {
      return defaultLocale.get();
    }

    return userLocale;
  }

  private static Optional<Locale> getDefaultLocale(DataFetchingEnvironment environment) {
    Map<String, ?> localContext = environment.getLocalContext();
    if (localContext == null) {
      return Optional.empty();
    }
    return Optional.ofNullable((Locale) localContext.get("locale"));
  }

  private static boolean acceptAnyLocale(@Nonnull Locale locale) {
    return locale.getLanguage().equals("*");
  }
}
