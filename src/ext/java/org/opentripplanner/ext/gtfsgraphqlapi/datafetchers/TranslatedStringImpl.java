package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.LegacyGraphQLDataFetchers;

public class TranslatedStringImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLTranslatedString {

  @Override
  public DataFetcher<String> language() {
    return environment -> getSource(environment).getKey();
  }

  @Override
  public DataFetcher<String> text() {
    return environment -> getSource(environment).getValue();
  }

  private Map.Entry<String, String> getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
