package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Currency;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLDataFetchers;
import org.opentripplanner.transit.model.basic.Money;

public class MoneyImpl implements GraphQLDataFetchers.GraphQLMoney {

  @Override
  public DataFetcher<Double> amount() {
    return env -> getSource(env).fractionalAmount().doubleValue();
  }

  @Override
  public DataFetcher<Currency> currency() {
    return env -> getSource(env).currency();
  }

  private Money getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
