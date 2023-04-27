package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.model.fare.FareMedium;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.RiderCategory;
import org.opentripplanner.transit.model.basic.Money;

public class LegacyGraphQLFareProductImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLFareProduct {

  @Override
  public DataFetcher<String> id() {
    return env -> getSource(env).id().toString();
  }

  @Override
  public DataFetcher<FareMedium> medium() {
    return env -> getSource(env).medium();
  }

  @Override
  public DataFetcher<String> name() {
    return env -> getSource(env).name();
  }

  @Override
  public DataFetcher<Money> price() {
    return env -> getSource(env).amount();
  }

  @Override
  public DataFetcher<RiderCategory> riderCategory() {
    return env -> getSource(env).category();
  }

  private FareProduct getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
