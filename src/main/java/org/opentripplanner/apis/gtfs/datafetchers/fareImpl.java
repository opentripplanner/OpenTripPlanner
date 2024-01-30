package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.transit.model.basic.Money;

public class fareImpl implements GraphQLDataFetchers.GraphQLFare {

  @Override
  public DataFetcher<Integer> cents() {
    return environment -> ((Money) getSource(environment).get("fare")).minorUnitAmount();
  }

  @Override
  public DataFetcher<Iterable<Object>> components() {
    return environment -> List.of();
  }

  @Override
  public DataFetcher<String> currency() {
    return environment -> ((Money) getSource(environment).get("fare")).currency().getCurrencyCode();
  }

  @Override
  public DataFetcher<String> type() {
    return environment -> getSource(environment).get("name").toString();
  }

  private Map<String, Object> getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
