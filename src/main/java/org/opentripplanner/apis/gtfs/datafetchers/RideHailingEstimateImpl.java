package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.Duration;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.apis.gtfs.model.RideHailingProvider;
import org.opentripplanner.ext.ridehailing.model.RideEstimate;
import org.opentripplanner.transit.model.basic.Money;

public class RideHailingEstimateImpl implements GraphQLDataFetchers.GraphQLRideHailingEstimate {

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
  public DataFetcher<String> productName() {
    return env -> getSource(env).productName();
  }

  @Override
  public DataFetcher<RideHailingProvider> provider() {
    return env -> new RideHailingProvider(getSource(env).provider().name().toLowerCase());
  }

  private RideEstimate getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
