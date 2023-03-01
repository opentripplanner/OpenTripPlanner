package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;

public class LegacyGraphQLFareProductImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLFareProduct {

  @Override
  public DataFetcher<Object> price() {
    return env -> getSource(env).amount();
  }

  @Override
  public DataFetcher<Object> container() {
    return env -> getSource(env).container();
  }

  @Override
  public DataFetcher<String> id() {
    return env -> getSource(env).id().toString();
  }

  @Override
  public DataFetcher<String> name() {
    return env -> getSource(env).name();
  }

  @Override
  public DataFetcher<Object> riderCategory() {
    return env -> getSource(env).category();
  }

  private FareProduct getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
