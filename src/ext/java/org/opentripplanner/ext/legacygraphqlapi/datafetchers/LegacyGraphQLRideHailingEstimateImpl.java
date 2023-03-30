package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.ext.ridehailing.model.RideEstimate;
import org.opentripplanner.transit.model.basic.Money;

public class LegacyGraphQLRideHailingEstimateImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLRideHailingEstimate {

  @Override
  public DataFetcher<Duration> arrival() {
    return env -> getSource(env).arrival();
  }

  @Override
  public DataFetcher<Money> maxPrice() {
    return env -> getSource(env).maxPrice();
  }

  @Override
  public DataFetcher<Money> minPrice() {
    return env -> getSource(env).minPrice();
  }

  @Override
  public DataFetcher<String> name() {
    return env -> getSource(env).productName();
  }

  @Override
  public DataFetcher<String> provider() {
    return env -> getSource(env).provider().toString();
  }

  private RideEstimate getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
