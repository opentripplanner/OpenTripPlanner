package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.ext.fares.model.FareProductInstance;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;

public class LegacyGraphQLFareProductInstanceImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLFareProductInstance {

  @Override
  public DataFetcher<String> instanceId() {
    return env -> getSource(env).instanceId();
  }

  @Override
  public DataFetcher<FareProduct> product() {
    return env -> getSource(env).product();
  }

  private FareProductInstance getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
