package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.plan.FareProductInstance;

public class LegacyGraphQLFareProductInstanceImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLFareProductInstance {

  @Override
  public DataFetcher<Object> price() {
    return env -> getSource(env).product().amount();
  }

  @Override
  public DataFetcher<Object> container() {
    return env -> getSource(env).product().container();
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
