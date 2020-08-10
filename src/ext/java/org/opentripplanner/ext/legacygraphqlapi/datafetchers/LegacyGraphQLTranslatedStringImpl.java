package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;

import java.util.Map;

public class LegacyGraphQLTranslatedStringImpl
    implements LegacyGraphQLDataFetchers.LegacyGraphQLTranslatedString {

  @Override
  public DataFetcher<String> text() {
    return environment -> getSource(environment).getValue();
  }

  @Override
  public DataFetcher<String> language() {
    return environment -> getSource(environment).getKey();
  }

  private Map.Entry<String, String> getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
