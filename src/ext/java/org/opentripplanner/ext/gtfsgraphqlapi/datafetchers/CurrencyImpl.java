package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Currency;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.LegacyGraphQLDataFetchers;

public class CurrencyImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLCurrency {

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
