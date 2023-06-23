package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLDataFetchers;
import org.opentripplanner.ext.gtfsgraphqlapi.model.UnknownModel;

public class UnknownImpl implements GraphQLDataFetchers.GraphQLUnknown {

  @Override
  public DataFetcher<String> description() {
    return environment -> getSource(environment).getDescription();
  }

  private UnknownModel getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
