package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.model.UnknownModel;

public class UnknownImpl implements GraphQLDataFetchers.GraphQLUnknown {

  @Override
  public DataFetcher<String> description() {
    return environment -> getSource(environment).getDescription();
  }

  private UnknownModel getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
