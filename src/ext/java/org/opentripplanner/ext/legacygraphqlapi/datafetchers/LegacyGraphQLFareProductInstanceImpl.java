package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.fares.model.FareProductInstance;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;

public class LegacyGraphQLFareProductInstanceImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLFareProductInstance {

  @Override
  public DataFetcher<Object> price() {
    return env -> getSource(env).product().amount();
  }

  @Override
  public DataFetcher<Object> medium() {
    return env -> getSource(env).product().medium();
  }

  @Override
  public DataFetcher<String> id() {
    return env -> getSource(env).product().id().toString();
  }

  @Override
  public DataFetcher<String> instanceId() {
    return env -> getSource(env).instanceId();
  }

  @Override
  public DataFetcher<String> name() {
    return env -> getSource(env).product().name();
  }

  @Override
  public DataFetcher<Object> riderCategory() {
    return env -> getSource(env).product().category();
  }

  private FareProductInstance getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
