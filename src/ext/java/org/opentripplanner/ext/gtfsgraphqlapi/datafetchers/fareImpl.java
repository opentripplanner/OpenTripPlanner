package org.opentripplanner.ext.gtfsgraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.opentripplanner.ext.gtfsgraphqlapi.generated.GraphQLDataFetchers;
import org.opentripplanner.routing.core.FareComponent;
import org.opentripplanner.transit.model.basic.Money;

public class fareImpl implements GraphQLDataFetchers.GraphQLFare {

  @Override
  public DataFetcher<Integer> cents() {
    return environment -> ((Money) getSource(environment).get("fare")).minorUnitAmount();
  }

  @Override
  public DataFetcher<Iterable<FareComponent>> components() {
    return environment -> (Iterable<FareComponent>) getSource(environment).get("details");
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
