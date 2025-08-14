package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.model.fare.FareOffer;

public class FareProductUseImpl implements GraphQLDataFetchers.GraphQLFareProductUse {

  @Override
  public DataFetcher<String> id() {
    return env -> getSource(env).uniqueId();
  }

  @Override
  public DataFetcher<FareOffer> product() {
    return this::getSource;
  }

  private FareOffer getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
