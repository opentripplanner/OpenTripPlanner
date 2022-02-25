package org.opentripplanner.ext.transmodelapi;

import graphql.schema.DataFetchingEnvironment;
import java.util.Locale;
import java.util.Map;

public class TransmodelGraphQLUtils {

    public static Locale getLocale(DataFetchingEnvironment environment) {
      String argLang = environment.getArgument("language");
      if (argLang != null) {
        return Locale.forLanguageTag(argLang);
      }

      Map<String, ?> localContext = environment.getLocalContext();
      if (localContext != null && localContext.get("locale") != null) {
        return (Locale) localContext.get("locale");
      }

      return environment.getLocale();
    }
}
