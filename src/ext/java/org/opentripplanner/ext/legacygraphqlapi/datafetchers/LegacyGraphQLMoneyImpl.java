package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Currency;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.transit.model.basic.Money;

public class LegacyGraphQLMoneyImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLMoney {

  @Override
  public DataFetcher<Integer> value() {
    return env -> getSource(env).cents();
  }

  @Override
  public DataFetcher<Currency> currency() {
    return env -> getSource(env).currency();
  }

  private Money getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
