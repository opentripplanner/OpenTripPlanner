package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.gtfsgraphqlapi.model.LegacyGraphQLUnknownModel;

public class UnknownImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLUnknown {

  @Override
  public DataFetcher<String> description() {
    return environment -> getSource(environment).getDescription();
  }

  private LegacyGraphQLUnknownModel getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
