package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.routing.core.FareComponent;
import org.opentripplanner.routing.core.Money;

import java.util.Map;

public class LegacyGraphQLfareImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLFare {

  @Override
  public DataFetcher<String> type() {
    return environment -> (String) getSource(environment).get("name");
  }

  @Override
  public DataFetcher<String> currency() {
    return environment -> ((Money) getSource(environment).get("fare")).getCurrency().getCurrencyCode();
  }

  @Override
  public DataFetcher<Integer> cents() {
    return environment -> ((Money) getSource(environment).get("fare")).getCents();
  }

  @Override
  public DataFetcher<Iterable<FareComponent>> components() {
    return environment -> (Iterable<FareComponent>) getSource(environment).get("details");
  }

  private Map<String, Object> getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
