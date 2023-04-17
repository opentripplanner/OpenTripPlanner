package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Currency;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.transit.model.basic.Money;

public class LegacyGraphQLCurrencyImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLCurrency {

  @Override
  public DataFetcher<String> code() {
    return env -> getSource(env).getCurrencyCode();
  }

  @Override
  public DataFetcher<Integer> digits() {
    return env -> getSource(env).getDefaultFractionDigits();
  }

  private Currency getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
