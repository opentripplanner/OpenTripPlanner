package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.FareProductUse;

public class FareProductUseImpl implements GraphQLDataFetchers.GraphQLFareProductUse {

  @Override
  public DataFetcher<String> id() {
    return env -> getSource(env).id();
  }

  @Override
  public DataFetcher<FareProduct> product() {
    return env -> getSource(env).product();
  }

  private FareProductUse getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
