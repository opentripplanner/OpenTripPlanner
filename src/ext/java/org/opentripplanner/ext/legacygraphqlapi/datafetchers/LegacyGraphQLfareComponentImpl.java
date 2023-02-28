package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import java.util.List;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.transit.model.network.Route;

public class LegacyGraphQLfareComponentImpl
  implements LegacyGraphQLDataFetchers.LegacyGraphQLFareComponent {

  @Override
  public DataFetcher<Integer> cents() {
    return environment -> null;
  }

  @Override
  public DataFetcher<String> currency() {
    return environment -> null;
  }

  @Override
  public DataFetcher<String> fareId() {
    return environment -> null;
  }

  @Override
  public DataFetcher<Iterable<Route>> routes() {
    return environment -> List.of();
  }
}
