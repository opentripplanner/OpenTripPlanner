package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.FareProductUse;

public class LegacyGraphQLFareProductUseImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLFareProductUse {

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
